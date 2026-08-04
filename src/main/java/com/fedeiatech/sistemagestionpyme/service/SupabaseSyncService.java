package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
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

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");

    private static SupabaseSyncService instancia;

    private final ItemDAO itemDAO;
    private final ConfiguracionDAO configDAO;
    /** Solo para tests — si es null, se construye un {@link JdkPostgrestClient} por sincronización con la URL vigente. */
    private final PostgrestClient clienteInyectado;

    private ScheduledExecutorService scheduler;

    private SupabaseSyncService() {
        this(new ItemDAO(), new ConfiguracionDAO(), null);
    }

    SupabaseSyncService(ItemDAO itemDAO, ConfiguracionDAO configDAO, PostgrestClient clienteInyectado) {
        this.itemDAO = itemDAO;
        this.configDAO = configDAO;
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

        List<String> pendientesEliminados = itemDAO.listarCodigosEliminadosPendientes();
        if (!pendientesEliminados.isEmpty()) {
            String payloadTombstones = serializarTombstones(pendientesEliminados);
            PostgrestResponse respTombstones = http.post("/rest/v1/products?on_conflict=sku", payloadTombstones, headers);
            if (respTombstones.esExitosa()) {
                itemDAO.limpiarEliminadosSincronizados(pendientesEliminados);
            }
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

    private static String serializarTombstones(List<String> codigos) {
        String syncedAt = Instant.now().toString();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < codigos.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"sku\":\"").append(escapeJson(codigos.get(i)))
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
