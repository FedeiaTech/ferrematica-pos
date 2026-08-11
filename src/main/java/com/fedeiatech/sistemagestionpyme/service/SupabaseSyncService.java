package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sincroniza el catálogo local (SQLite) con la tabla {@code public.products} de Supabase
 * vía PostgREST. Singleton — sigue el mismo patrón que {@link SessionService}/{@link ThemeService}.
 *
 * <p>Flujo (ver Design doc "Data Flow"):
 * <ol>
 *   <li>{@link ItemDAO#validarCodigosParaSync()} — no vacío ⇒ {@link SyncBloqueadoException}, antes de
 *       cualquier llamada HTTP. Este es el gate autoritativo (el de la UI en ConfigController es solo
 *       informativo, este es el que realmente bloquea).</li>
 *   <li>{@link ItemDAO#listarTodos()} — snapshot. Los combos están estructuralmente ausentes: entran al
 *       dominio únicamente vía {@code ItemVenta.desdeCombo()} en los controllers (merges de vista), nunca
 *       en el DAO. No hace falta (ni corresponde) filtrarlos acá.</li>
 *   <li>Autenticación GoTrue como cuenta dedicada {@code rol='dueno'} — el JWT resultante viaja como
 *       {@code Authorization: Bearer}, la anon key se mantiene únicamente como header {@code apikey}
 *       (nunca escrituras solo-con-anon-key).</li>
 *   <li>Mapeo a JSON con la allow-list {@link #COLUMNAS_PUSH} — nunca {@code description}/{@code image_url}
 *       (propiedad de Web-Shop) ni {@code precio_costo} (nunca se sube).</li>
 *   <li>POST a {@code /rest/v1/products} con {@code Prefer: resolution=merge-duplicates} y
 *       {@code on_conflict=sku}.</li>
 *   <li>Tombstones: {@link ItemDAO#listarCodigosEliminadosPendientes()} se empujan como upserts
 *       {@code is_active=false}; en éxito HTTP 2xx se limpian con
 *       {@link ItemDAO#limpiarEliminadosSincronizados(List)}.</li>
 * </ol>
 * Nada de JavaFX acá — el estado/label de UI lo actualiza el llamador (ConfigController) vía
 * {@code Platform.runLater}, nunca este servicio.
 */
public class SupabaseSyncService {

    private static final Logger LOGGER = Logger.getLogger(SupabaseSyncService.class.getName());

    // Web-Shop es dueño de description/image_url — nunca se serializan acá. precio_costo nunca se sube.
    private static final List<String> COLUMNAS_PUSH = List.of(
        "sku", "name", "price", "stock", "unit", "category", "is_service", "is_active", "synced_at");

    // Allow-list de ventas/detalle_ventas — ver migración 0011_ventas_mirror.sql (design decision D1/D2/D3).
    private static final List<String> COLUMNAS_PUSH_VENTAS = List.of(
        "install_id", "venta_local_id", "fecha", "fecha_local", "total", "estado");
    private static final List<String> COLUMNAS_PUSH_DETALLE_VENTAS = List.of(
        "install_id", "venta_local_id", "detalle_local_id", "producto_codigo", "combo_id",
        "descripcion", "cantidad", "precio_unitario", "subtotal");

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");

    // Extracción por clave, no por posición — la migración 0013 amplió estado_envios_pos() con
    // pending_balance y el orden de columnas ya no es una garantía a la que atarse (design decision DA7).
    private static final Pattern P_VENTA_LOCAL_ID = Pattern.compile("\"venta_local_id\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern P_STATUS = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern P_UPDATED_AT = Pattern.compile("\"updated_at\"\\s*:\\s*\"([^\"]*)\"");
    // No matchea "null" ni ausencia de la clave (POS corriendo contra un backend sin la 0013 aplicada
    // todavía) — en ambos casos saldoPendiente queda en null, no se descarta la fila.
    private static final Pattern P_PENDING_BALANCE = Pattern.compile("\"pending_balance\"\\s*:\\s*\"?(-?\\d+(?:\\.\\d+)?)\"?");
    // Misma lógica de "no matchea null/ausencia" que P_PENDING_BALANCE — 0014 amplía estado_envios_pos()
    // con delivered_at, un POS contra un backend pre-0014 simplemente deja entregadoEn en null.
    private static final Pattern P_DELIVERED_AT = Pattern.compile("\"delivered_at\"\\s*:\\s*\"([^\"]*)\"");

    private static SupabaseSyncService instancia;

    private final ItemDAO itemDAO;
    private final ConfiguracionDAO configDAO;
    private final VentaDAO ventaDAO;
    /** Solo para tests — si es null, se construye un {@link JdkPostgrestClient} por sincronización con la URL vigente. */
    private final PostgrestClient clienteInyectado;

    private ScheduledExecutorService scheduler;

    private SupabaseSyncService() {
        this(new ItemDAO(), new ConfiguracionDAO(), new VentaDAO(), null);
    }

    SupabaseSyncService(ItemDAO itemDAO, ConfiguracionDAO configDAO, PostgrestClient clienteInyectado) {
        this(itemDAO, configDAO, new VentaDAO(), clienteInyectado);
    }

    SupabaseSyncService(ItemDAO itemDAO, ConfiguracionDAO configDAO, VentaDAO ventaDAO, PostgrestClient clienteInyectado) {
        this.itemDAO = itemDAO;
        this.configDAO = configDAO;
        this.ventaDAO = ventaDAO;
        this.clienteInyectado = clienteInyectado;
    }

    public static synchronized SupabaseSyncService getInstance() {
        if (instancia == null) instancia = new SupabaseSyncService();
        return instancia;
    }

    /**
     * Ejecuta una sincronización completa. Lanza {@link SyncBloqueadoException} (sin tocar la red) si hay
     * códigos inválidos, {@link IllegalStateException} si falta configuración, o {@link IOException}/
     * {@link SQLException} ante errores de red o de base de datos.
     */
    public void sincronizar() throws SQLException, IOException {
        List<String> bloqueados = itemDAO.validarCodigosParaSync();
        if (!bloqueados.isEmpty()) {
            throw new SyncBloqueadoException(bloqueados);
        }

        Configuracion config = configDAO.obtenerConfiguracion();
        if (config == null || esVacio(config.getSupabaseUrl()) || esVacio(config.getSupabaseAnonKey())
                || esVacio(config.getSupabaseSyncEmail()) || esVacio(config.getSupabaseSyncPassword())) {
            throw new IllegalStateException("La configuración de sincronización con Supabase está incompleta.");
        }

        PostgrestClient http = clienteInyectado != null ? clienteInyectado : new JdkPostgrestClient(config.getSupabaseUrl());

        String jwt = autenticar(http, config);
        Map<String, String> headers = construirHeadersProducts(config.getSupabaseAnonKey(), jwt);

        List<ItemVenta> items = itemDAO.listarTodos(); // combos estructuralmente ausentes — ver Javadoc de clase
        String payloadItems = serializarItems(items);
        PostgrestResponse respItems = http.post("/rest/v1/products?on_conflict=sku", payloadItems, headers);
        if (!respItems.esExitosa()) {
            throw new IOException("Error al sincronizar productos: HTTP " + respItems.statusCode() + " - " + respItems.body());
        }

        List<ItemDAO.ItemEliminado> pendientesEliminados = itemDAO.listarEliminadosConNombrePendientes();
        if (!pendientesEliminados.isEmpty()) {
            String payloadTombstones = serializarTombstones(pendientesEliminados);
            PostgrestResponse respTombstones = http.post("/rest/v1/products?on_conflict=sku", payloadTombstones, headers);
            if (respTombstones.esExitosa()) {
                List<String> codigos = pendientesEliminados.stream().map(ItemDAO.ItemEliminado::codigo).toList();
                itemDAO.limpiarEliminadosSincronizados(codigos);
            } else {
                LOGGER.log(Level.WARNING, "Fallo al empujar tombstones a Supabase: HTTP "
                        + respTombstones.statusCode() + " - " + respTombstones.body());
            }
        }

        if (config.isSupabaseSyncVentasHabilitado()) {
            sincronizarVentas(http, headers);
        }

        configDAO.actualizarUltimaSincronizacionExitosa(Instant.now().toString());
    }

    /**
     * Empuja las ventas locales pendientes ({@code sincronizada_en IS NULL}) y sus detalles a
     * {@code public.ventas}/{@code public.detalle_ventas}, con la misma cuenta de servicio y JWT
     * ya autenticados por {@link #sincronizar()}. Nunca lanza — un fallo acá (red, RLS, lo que
     * sea) se loguea como WARNING y no debe invalidar una sincronización de productos exitosa
     * (mismo criterio que el bloque de tombstones, design decision D5).
     */
    private void sincronizarVentas(PostgrestClient http, Map<String, String> headers) {
        try {
            List<VentaDAO.VentaPendiente> pendientes = ventaDAO.listarVentasPendientesDeSync();
            if (pendientes.isEmpty()) return;

            String installId = configDAO.obtenerOGenerarInstallId();

            String payloadVentas = serializarVentas(pendientes, installId);
            PostgrestResponse respVentas = http.post(
                "/rest/v1/ventas?on_conflict=install_id,venta_local_id", payloadVentas, headers);
            if (!respVentas.esExitosa()) {
                throw new IOException("Error al sincronizar ventas: HTTP "
                    + respVentas.statusCode() + " - " + respVentas.body());
            }

            String payloadDetalles = serializarDetalleVentas(pendientes, installId);
            if (!payloadDetalles.equals("[]")) {
                PostgrestResponse respDetalles = http.post(
                    "/rest/v1/detalle_ventas?on_conflict=install_id,venta_local_id,detalle_local_id",
                    payloadDetalles, headers);
                if (!respDetalles.esExitosa()) {
                    throw new IOException("Error al sincronizar detalles de venta: HTTP "
                        + respDetalles.statusCode() + " - " + respDetalles.body());
                }
            }

            List<Integer> idsSincronizados = pendientes.stream().map(VentaDAO.VentaPendiente::id).toList();
            ventaDAO.marcarVentasSincronizadas(idsSincronizados);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fallo al empujar ventas a Supabase", e);
        }
    }

    /**
     * Estado de envío de una venta, resuelto vía el RPC {@code estado_envios_pos()} (migración 0012,
     * ampliada por 0013 con {@code saldoPendiente} y por 0014 con {@code entregadoEn}).
     * {@code saldoPendiente == null} significa pagado por completo o cobro aún no registrado —
     * nunca implica que la columna no exista. {@code entregadoEn == null} significa que el pedido
     * todavía no fue marcado como entregado, o que el backend no tiene la 0014 aplicada todavía.
     */
    public record EstadoEnvio(String status, Instant actualizadoEn, Double saldoPendiente, Instant entregadoEn) {}

    /**
     * Resultado de {@link #obtenerEstadoEnvios()} — separa explícitamente "no se pudo consultar"
     * de "se consultó bien y no hay envíos vinculados", algo que un {@code Map} vacío por sí solo
     * no puede distinguir. {@code exitoso=false} es la señal para mostrar el aviso no bloqueante en
     * Reportes; {@code exitoso=true} con {@code estados} vacío es un resultado válido y NO debe
     * mostrar ningún aviso.
     */
    public record ResultadoEstadoEnvios(Map<Integer, EstadoEnvio> estados, boolean exitoso) {
        static ResultadoEstadoEnvios fallo() {
            return new ResultadoEstadoEnvios(Map.of(), false);
        }

        static ResultadoEstadoEnvios exito(Map<Integer, EstadoEnvio> estados) {
            return new ResultadoEstadoEnvios(estados, true);
        }
    }

    /**
     * Consulta el RPC {@code estado_envios_pos()} y devuelve el estado de envío de cada venta
     * vinculada a un pedido, en una ventana fija de 90 días resuelta server-side. Nunca lanza:
     * ante red caída, configuración incompleta, POS sin provisionar ({@code pos_installs}, HTTP 42501)
     * o cualquier otro fallo, devuelve {@link ResultadoEstadoEnvios#fallo()} y loguea WARNING —
     * mismo criterio que {@link #sincronizarVentas}, pero marcando el fallo explícitamente en vez
     * de devolver un mapa vacío indistinguible de "no hay envíos". Llamar FUERA del hilo FX (el
     * caller hace {@code Platform.runLater} con el resultado).
     */
    public ResultadoEstadoEnvios obtenerEstadoEnvios() {
        try {
            Configuracion config = configDAO.obtenerConfiguracion();
            if (config == null || esVacio(config.getSupabaseUrl()) || esVacio(config.getSupabaseAnonKey())
                    || esVacio(config.getSupabaseSyncEmail()) || esVacio(config.getSupabaseSyncPassword())) {
                return ResultadoEstadoEnvios.fallo();
            }

            PostgrestClient http = clienteInyectado != null ? clienteInyectado : new JdkPostgrestClient(config.getSupabaseUrl());
            String jwt = autenticar(http, config);
            Map<String, String> headers = construirHeadersRpc(config.getSupabaseAnonKey(), jwt);

            PostgrestResponse resp = http.post("/rest/v1/rpc/estado_envios_pos", "{}", headers);
            if (!resp.esExitosa()) {
                LOGGER.log(Level.WARNING, "No se pudo obtener el estado de envíos: HTTP "
                    + resp.statusCode() + " - " + resp.body());
                return ResultadoEstadoEnvios.fallo();
            }
            return ResultadoEstadoEnvios.exito(parsearEstadoEnvios(resp.body()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fallo al consultar el estado de envíos en Supabase", e);
            return ResultadoEstadoEnvios.fallo();
        }
    }

    private static Map<Integer, EstadoEnvio> parsearEstadoEnvios(String jsonBody) {
        Map<Integer, EstadoEnvio> resultado = new LinkedHashMap<>();
        if (jsonBody == null) return resultado;
        for (String objeto : dividirObjetos(jsonBody)) {
            Matcher mId = P_VENTA_LOCAL_ID.matcher(objeto);
            Matcher mStatus = P_STATUS.matcher(objeto);
            if (!mId.find() || !mStatus.find()) {
                LOGGER.log(Level.FINE, "Objeto de estado_envios_pos sin venta_local_id o status, descartado: " + objeto);
                continue;
            }

            int ventaLocalId;
            try {
                ventaLocalId = Integer.parseInt(mId.group(1));
            } catch (NumberFormatException e) {
                LOGGER.log(Level.FINE, "venta_local_id no parseable en estado_envios_pos, descartado: " + objeto, e);
                continue;
            }
            String status = mStatus.group(1);

            Instant actualizadoEn = null;
            Matcher mUpdatedAt = P_UPDATED_AT.matcher(objeto);
            if (mUpdatedAt.find()) {
                try {
                    actualizadoEn = OffsetDateTime.parse(mUpdatedAt.group(1)).toInstant();
                } catch (DateTimeParseException e) {
                    LOGGER.log(Level.FINE, "updated_at no parseable en estado_envios_pos para venta " + ventaLocalId, e);
                }
            }

            Double saldoPendiente = null;
            Matcher mSaldo = P_PENDING_BALANCE.matcher(objeto);
            if (mSaldo.find()) {
                try {
                    saldoPendiente = Double.parseDouble(mSaldo.group(1));
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.FINE, "pending_balance no parseable en estado_envios_pos para venta " + ventaLocalId, e);
                }
            }

            Instant entregadoEn = null;
            Matcher mDeliveredAt = P_DELIVERED_AT.matcher(objeto);
            if (mDeliveredAt.find() && !mDeliveredAt.group(1).isEmpty()) {
                try {
                    entregadoEn = OffsetDateTime.parse(mDeliveredAt.group(1)).toInstant();
                } catch (DateTimeParseException e) {
                    LOGGER.log(Level.FINE, "delivered_at no parseable en estado_envios_pos para venta " + ventaLocalId, e);
                }
            }

            resultado.put(ventaLocalId, new EstadoEnvio(status, actualizadoEn, saldoPendiente, entregadoEn));
        }
        return resultado;
    }

    /**
     * Separa el array JSON del RPC en sus objetos de nivel superior, sin depender de una librería
     * JSON (pom.xml no tiene ninguna — design decision DA7). Rastrea profundidad de llaves y estado
     * de string (con escapes) para que un futuro valor de texto con {@code {}} no desincronice el
     * corte. Package-private para poder testearlo directamente.
     */
    static List<String> dividirObjetos(String json) {
        List<String> objetos = new java.util.ArrayList<>();
        if (json == null) return objetos;
        int profundidad = 0;
        boolean enString = false;
        boolean escapado = false;
        int inicio = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (enString) {
                if (escapado) {
                    escapado = false;
                } else if (c == '\\') {
                    escapado = true;
                } else if (c == '"') {
                    enString = false;
                }
                continue;
            }
            if (c == '"') {
                enString = true;
            } else if (c == '{') {
                if (profundidad == 0) inicio = i;
                profundidad++;
            } else if (c == '}') {
                profundidad--;
                if (profundidad == 0 && inicio >= 0) {
                    objetos.add(json.substring(inicio, i + 1));
                    inicio = -1;
                }
            }
        }
        return objetos;
    }

    private static Map<String, String> construirHeadersRpc(String anonKey, String jwt) {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", anonKey);
        headers.put("Authorization", "Bearer " + jwt);
        return headers;
    }

    /**
     * Momento de la última sincronización exitosa, persistido en {@code configuracion} — sobrevive
     * un reinicio de la app. {@code null} si nunca sincronizó con éxito, o si el valor guardado no
     * es parseable.
     */
    public Instant ultimaSincronizacionExitosaEn() {
        try {
            Configuracion config = configDAO.obtenerConfiguracion();
            String valor = config != null ? config.getSupabaseUltimaSyncExitosa() : null;
            return valor == null || valor.isBlank() ? null : Instant.parse(valor);
        } catch (SQLException | java.time.format.DateTimeParseException e) {
            LOGGER.log(Level.WARNING, "No se pudo leer la última sincronización exitosa persistida", e);
            return null;
        }
    }

    /** Arranca (o reprograma) el scheduler periódico según la configuración vigente; no hace nada si está deshabilitado. */
    public synchronized void iniciarProgramacionSiCorresponde() {
        detenerProgramacion();
        try {
            Configuracion config = configDAO.obtenerConfiguracion();
            if (config == null || !config.isSupabaseSyncHabilitado()) return;

            int intervaloMin = config.getSupabaseSyncIntervaloMin() > 0 ? config.getSupabaseSyncIntervaloMin() : 15;
            scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread hilo = new Thread(runnable, "supabase-sync-scheduler");
                hilo.setDaemon(true);
                return hilo;
            });
            scheduler.scheduleAtFixedRate(this::ejecutarSincronizacionProgramada, intervaloMin, intervaloMin, TimeUnit.MINUTES);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "No se pudo leer la configuración para programar la sincronización con Supabase", e);
        }
    }

    /** Detiene el scheduler si está corriendo. Seguro de llamar aunque no haya nada programado. */
    public synchronized void detenerProgramacion() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void ejecutarSincronizacionProgramada() {
        try {
            sincronizar();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en sincronización programada con Supabase", e);
        }
    }

    private String autenticar(PostgrestClient http, Configuracion config) throws IOException {
        String body = "{\"email\":\"" + escapeJson(config.getSupabaseSyncEmail())
            + "\",\"password\":\"" + escapeJson(config.getSupabaseSyncPassword()) + "\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", config.getSupabaseAnonKey());

        PostgrestResponse resp = http.post("/auth/v1/token?grant_type=password", body, headers);
        if (!resp.esExitosa()) {
            throw new IOException("No se pudo autenticar con Supabase: HTTP " + resp.statusCode() + " - " + resp.body());
        }
        String token = extraerAccessToken(resp.body());
        if (token == null) {
            throw new IOException("Respuesta de autenticación de Supabase sin access_token: " + resp.body());
        }
        return token;
    }

    private static String extraerAccessToken(String jsonBody) {
        if (jsonBody == null) return null;
        Matcher m = ACCESS_TOKEN_PATTERN.matcher(jsonBody);
        return m.find() ? m.group(1) : null;
    }

    private static Map<String, String> construirHeadersProducts(String anonKey, String jwt) {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", anonKey);
        headers.put("Authorization", "Bearer " + jwt);
        headers.put("Prefer", "resolution=merge-duplicates");
        return headers;
    }

    private static String serializarItems(List<ItemVenta> items) {
        String syncedAt = Instant.now().toString();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializarItem(items.get(i), syncedAt));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String serializarItem(ItemVenta item, String syncedAt) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"sku\":\"").append(escapeJson(item.getCodigo())).append("\",");
        sb.append("\"name\":\"").append(escapeJson(item.getNombre())).append("\",");
        sb.append("\"price\":").append(item.getPrecioVenta()).append(",");
        sb.append("\"stock\":").append(item.getStock()).append(",");
        sb.append("\"unit\":\"").append(escapeJson(item.getUnidad())).append("\",");
        sb.append("\"category\":\"").append(escapeJson(item.getCategoria())).append("\",");
        sb.append("\"is_service\":").append(item.isEsServicio()).append(",");
        sb.append("\"is_active\":true,");
        sb.append("\"synced_at\":\"").append(syncedAt).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String serializarVentas(List<VentaDAO.VentaPendiente> pendientes, String installId) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pendientes.size(); i++) {
            if (i > 0) sb.append(",");
            VentaDAO.VentaPendiente venta = pendientes.get(i);
            String fechaUtc = convertirFechaLocalAUtc(venta.fecha()).toString();
            sb.append("{");
            sb.append("\"install_id\":\"").append(escapeJson(installId)).append("\",");
            sb.append("\"venta_local_id\":").append(venta.id()).append(",");
            sb.append("\"fecha\":\"").append(fechaUtc).append("\",");
            sb.append("\"fecha_local\":\"").append(escapeJson(venta.fecha())).append("\",");
            sb.append("\"total\":").append(venta.total()).append(",");
            sb.append("\"estado\":\"").append(escapeJson(venta.estado())).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String serializarDetalleVentas(List<VentaDAO.VentaPendiente> pendientes, String installId) {
        StringBuilder sb = new StringBuilder("[");
        boolean primero = true;
        for (VentaDAO.VentaPendiente venta : pendientes) {
            for (VentaDAO.DetallePendiente detalle : venta.detalles()) {
                if (!primero) sb.append(",");
                primero = false;
                sb.append("{");
                sb.append("\"install_id\":\"").append(escapeJson(installId)).append("\",");
                sb.append("\"venta_local_id\":").append(venta.id()).append(",");
                sb.append("\"detalle_local_id\":").append(detalle.id()).append(",");
                sb.append("\"producto_codigo\":").append(jsonStringOrNull(detalle.productoCodigo())).append(",");
                sb.append("\"combo_id\":").append(detalle.comboId() != null ? detalle.comboId() : "null").append(",");
                sb.append("\"descripcion\":\"").append(escapeJson(detalle.descripcion())).append("\",");
                sb.append("\"cantidad\":").append(detalle.cantidad()).append(",");
                sb.append("\"precio_unitario\":").append(detalle.precioUnitario()).append(",");
                sb.append("\"subtotal\":").append(detalle.subtotal());
                sb.append("}");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String jsonStringOrNull(String valor) {
        return valor == null ? "null" : "\"" + escapeJson(valor) + "\"";
    }

    /**
     * Convierte la {@code fecha} local (zone-less, tal como se guarda en SQLite) a UTC en el
     * momento del push, usando el huso horario del equipo (design decision D3). El valor local
     * en SQLite nunca se reescribe — este método solo produce el {@code timestamptz} que viaja
     * a Supabase. Acepta tanto {@code yyyy-MM-dd} (fecha sin hora) como
     * {@code yyyy-MM-dd'T'HH:mm:ss} (con hora).
     */
    private static Instant convertirFechaLocalAUtc(String fechaLocal) {
        LocalDateTime fechaHora;
        try {
            fechaHora = LocalDateTime.parse(fechaLocal);
        } catch (DateTimeParseException e) {
            fechaHora = LocalDate.parse(fechaLocal).atStartOfDay();
        }
        return fechaHora.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static String serializarTombstones(List<ItemDAO.ItemEliminado> pendientes) {
        String syncedAt = Instant.now().toString();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pendientes.size(); i++) {
            if (i > 0) sb.append(",");
            ItemDAO.ItemEliminado item = pendientes.get(i);
            // "name" NOT NULL en products — un upsert ON CONFLICT DO UPDATE igual valida la fila
            // insertada antes de resolver el conflicto, así que hace falta mandarlo aunque solo
            // nos importe actualizar is_active. Si no quedó nombre guardado, el sku sirve de respaldo.
            String nombre = item.nombre() != null && !item.nombre().isBlank() ? item.nombre() : item.codigo();
            sb.append("{\"sku\":\"").append(escapeJson(item.codigo()))
              .append("\",\"name\":\"").append(escapeJson(nombre))
              .append("\",\"is_active\":false,\"synced_at\":\"").append(syncedAt).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }
}
