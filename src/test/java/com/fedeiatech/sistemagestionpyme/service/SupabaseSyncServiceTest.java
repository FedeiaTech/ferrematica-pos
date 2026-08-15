package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ComboDAO;
import com.fedeiatech.sistemagestionpyme.dao.ConexionDB;
import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Combo;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupabaseSyncServiceTest {

    private final ItemDAO itemDAO = new ItemDAO();
    private final ConfiguracionDAO configDAO = new ConfiguracionDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    @BeforeEach
    void setUp(@TempDir File tempDir) throws SQLException {
        System.setProperty("db.path", new File(tempDir, "test.db").getAbsolutePath());
        ConexionDB.resetParaTests();
        configDAO.inicializarTabla();
    }

    @AfterEach
    void tearDown() throws SQLException {
        ConexionDB.resetParaTests();
        System.clearProperty("db.path");
    }

    @Test
    void payloadIncluyeSoloLasColumnasPermitidas() throws Exception {
        itemDAO.guardar(new ItemVenta(0, "SKU-1", "Martillo", "descripción oculta", 50.0, 100.0, 10.0, false));
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        service.sincronizar();

        String body = fake.buscarLlamadaProducts().jsonBody();
        assertTrue(body.contains("\"sku\":\"SKU-1\""));
        assertTrue(body.contains("\"name\":\"Martillo\""));
        assertTrue(body.contains("\"price\":100.0"));
        assertTrue(body.contains("\"stock\":10.0"));
        assertTrue(body.contains("\"unit\":\"u\""));
        assertTrue(body.contains("\"category\":\"General\""));
        assertTrue(body.contains("\"is_service\":false"));
        assertTrue(body.contains("\"is_active\":true"));
        assertTrue(body.contains("\"synced_at\""));
        assertFalse(body.contains("description"));
        assertFalse(body.contains("image_url"));
        assertFalse(body.contains("precio_costo"));
        assertFalse(body.contains("descripción oculta"));
    }

    @Test
    void abortaAntesDeCualquierLlamadaHttpSiHayCodigosInvalidos() throws Exception {
        insertarItemConCodigoCrudo("", "Producto sin codigo");
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        assertThrows(SyncBloqueadoException.class, service::sincronizar);
        assertTrue(fake.llamadas.isEmpty());
    }

    @Test
    void incluyeCombosEnElSnapshotConPrefijoDeSkuYSinStockPropio() throws Exception {
        itemDAO.guardar(new ItemVenta(0, "SKU-2", "Tornillo", "d", 1.0, 2.0, 100.0, false));
        Combo combo = new Combo("KIT-1", "Kit de arranque", "d", 500.0);
        new ComboDAO().guardar(combo);
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        service.sincronizar();

        String body = fake.buscarLlamadaProducts().jsonBody();
        assertEquals(1, itemDAO.listarTodos().size());
        assertTrue(body.contains("\"sku\":\"SKU-2\""));
        // sku con prefijo "COMBO-": combos.codigo no comparte espacio de unicidad con items.codigo
        // en SQLite, así que el prefijo evita colisionar con un item que use el mismo código.
        assertTrue(body.contains("\"sku\":\"COMBO-KIT-1\""));
        assertTrue(body.contains("\"name\":\"Kit de arranque\""));
        assertTrue(body.contains("\"price\":500.0"));
        assertTrue(body.contains("\"category\":\"Combos\""));
        assertTrue(body.contains("\"is_combo\":true"));
        // Un combo no tiene stock propio (se resuelve de sus componentes solo dentro del POS al
        // vender) — viaja con stock=0/unit="u" fijos; Web-Shop no debe mostrar ese número.
        int comboStart = body.indexOf("\"sku\":\"COMBO-KIT-1\"");
        String comboObjeto = body.substring(comboStart, body.indexOf('}', comboStart) + 1);
        assertTrue(comboObjeto.contains("\"stock\":0"));
        assertTrue(comboObjeto.contains("\"unit\":\"u\""));
    }

    @Test
    void escapaCaracteresEspecialesEnElNombreYRoundTripea() throws Exception {
        String nombreConEspeciales = "Llave \"10\\12\" \nnueva";
        itemDAO.guardar(new ItemVenta(0, "SKU-3", nombreConEspeciales, "d", 1.0, 2.0, 1.0, false));
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        service.sincronizar();

        String body = fake.buscarLlamadaProducts().jsonBody();
        assertEquals(nombreConEspeciales, extraerCampoString(body, "name"));
    }

    @Test
    void tombstonesIncluyenNombreParaNoViolarNotNullEnSupabase() throws Exception {
        itemDAO.guardar(new ItemVenta(0, "SKU-4", "Pala punta cuadrada", "d", 5.0, 10.0, 3.0, false));
        ItemVenta guardado = itemDAO.buscarPorCodigo("SKU-4");
        itemDAO.eliminar(guardado.getId());
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        service.sincronizar();

        FakePostgrestClient.Llamada llamadaTombstone = fake.llamadas.stream()
            .filter(l -> l.path().startsWith("/rest/v1/products") && l.jsonBody().contains("\"is_active\":false"))
            .findFirst().orElseThrow();
        assertTrue(llamadaTombstone.jsonBody().contains("\"sku\":\"SKU-4\""));
        assertTrue(llamadaTombstone.jsonBody().contains("\"name\":\"Pala punta cuadrada\""));
        assertTrue(itemDAO.listarCodigosEliminadosPendientes().isEmpty());
    }

    @Test
    void ventaPendienteSeEmpujaYQuedaMarcadaComoSincronizada() throws Exception {
        itemDAO.guardar(new ItemVenta(0, "SKU-V1", "Pala", "d", 5.0, 100.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("SKU-V1");
        Venta venta = new Venta();
        venta.setFecha("2026-07-28T10:00:00");
        venta.agregarDetalle(new DetalleVenta(item, 2.0));
        ventaDAO.registrarVenta(venta);
        assertEquals(1, ventaDAO.listarVentasPendientesDeSync().size());

        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, ventaDAO, fake);

        service.sincronizar();

        FakePostgrestClient.Llamada llamadaVentas = fake.buscarLlamada("/rest/v1/ventas");
        assertTrue(llamadaVentas.jsonBody().contains("\"venta_local_id\":" + venta.getId()));
        assertTrue(llamadaVentas.jsonBody().contains("\"total\":200.0"));
        assertTrue(llamadaVentas.jsonBody().contains("\"estado\":\"completada\""));
        assertTrue(llamadaVentas.jsonBody().contains("\"fecha_local\":\"2026-07-28T10:00:00\""));
        assertTrue(llamadaVentas.jsonBody().contains("\"install_id\""));

        FakePostgrestClient.Llamada llamadaDetalles = fake.buscarLlamada("/rest/v1/detalle_ventas");
        assertTrue(llamadaDetalles.jsonBody().contains("\"producto_codigo\":\"SKU-V1\""));
        assertTrue(llamadaDetalles.jsonBody().contains("\"cantidad\":2.0"));
        assertTrue(llamadaDetalles.jsonBody().contains("\"combo_id\":null"));

        assertTrue(ventaDAO.listarVentasPendientesDeSync().isEmpty(),
                "Tras un push exitoso la venta ya no debe listarse como pendiente");
    }

    @Test
    void fechaLocalSeConvierteAUtcUsandoElHusoDelEquipo() throws Exception {
        itemDAO.guardar(new ItemVenta(0, "SKU-V2", "Pinza", "d", 5.0, 50.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("SKU-V2");
        Venta venta = new Venta();
        String fechaLocal = "2026-07-28T10:00:00";
        venta.setFecha(fechaLocal);
        venta.agregarDetalle(new DetalleVenta(item, 1.0));
        ventaDAO.registrarVenta(venta);

        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, ventaDAO, fake);

        service.sincronizar();

        String fechaUtcEsperada = LocalDateTime.parse(fechaLocal).atZone(ZoneId.systemDefault()).toInstant().toString();
        String body = fake.buscarLlamada("/rest/v1/ventas").jsonBody();
        assertTrue(body.contains("\"fecha\":\"" + fechaUtcEsperada + "\""));
    }

    @Test
    void obtenerEstadoEnviosParseaLaRespuestaDelRpc() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(200,
            "[{\"venta_local_id\":1,\"status\":\"entregado\",\"updated_at\":\"2026-08-01T10:00:00+00:00\"},"
            + "{\"venta_local_id\":2,\"status\":\"pendiente\",\"updated_at\":\"2026-08-05T12:30:00Z\"}]");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();

        assertTrue(resultado.exitoso());
        assertEquals(2, resultado.estados().size());
        assertEquals("entregado", resultado.estados().get(1).status());
        assertEquals("pendiente", resultado.estados().get(2).status());
        assertEquals(java.time.Instant.parse("2026-08-01T10:00:00Z"), resultado.estados().get(1).actualizadoEn());
        FakePostgrestClient.Llamada llamadaRpc = fake.buscarLlamada("/rest/v1/rpc/estado_envios_pos");
        assertFalse(llamadaRpc.headers().containsKey("Prefer"));
    }

    @Test
    void obtenerEstadoEnviosMarcaFalloSiElRpcFalla() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(403, "{\"code\":\"42501\"}");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();
        assertFalse(resultado.exitoso());
        assertTrue(resultado.estados().isEmpty());
    }

    @Test
    void obtenerEstadoEnviosMarcaFalloSiFaltaConfiguracion() {
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();
        assertFalse(resultado.exitoso());
        assertTrue(resultado.estados().isEmpty());
        assertTrue(fake.llamadas.isEmpty());
    }

    @Test
    void obtenerEstadoEnviosMarcaExitoConMapaVacioSiNoHayEnviosVinculados() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(200, "[]");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();
        assertTrue(resultado.exitoso());
        assertTrue(resultado.estados().isEmpty());
    }

    @Test
    void obtenerEstadoEnviosParseaConPendingBalance() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(200,
            "[{\"venta_local_id\":1,\"status\":\"entregado\",\"updated_at\":\"2026-08-01T10:00:00Z\",\"pending_balance\":60.00}]");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();

        assertTrue(resultado.exitoso());
        assertEquals(60.00, resultado.estados().get(1).saldoPendiente());
    }

    @Test
    void obtenerEstadoEnviosParseaCamposEnOrdenDistinto() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(200,
            "[{\"status\":\"entregado\",\"pending_balance\":60.00,\"venta_local_id\":1,\"updated_at\":\"2026-08-01T10:00:00Z\"}]");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();

        assertTrue(resultado.exitoso());
        assertEquals(1, resultado.estados().size());
        assertEquals("entregado", resultado.estados().get(1).status());
        assertEquals(60.00, resultado.estados().get(1).saldoPendiente());
        assertEquals(java.time.Instant.parse("2026-08-01T10:00:00Z"), resultado.estados().get(1).actualizadoEn());
    }

    @Test
    void obtenerEstadoEnviosSinColumnaPendingBalanceDejaSaldoEnNull() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(200,
            "[{\"venta_local_id\":1,\"status\":\"entregado\",\"updated_at\":\"2026-08-01T10:00:00Z\"}]");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();

        assertTrue(resultado.exitoso());
        assertEquals(1, resultado.estados().size());
        assertEquals(null, resultado.estados().get(1).saldoPendiente());
    }

    @Test
    void obtenerEstadoEnviosConPendingBalanceNuloDejaSaldoEnNull() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(200,
            "[{\"venta_local_id\":1,\"status\":\"entregado\",\"updated_at\":\"2026-08-01T10:00:00Z\",\"pending_balance\":null}]");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();

        assertTrue(resultado.exitoso());
        assertEquals(1, resultado.estados().size());
        assertEquals(null, resultado.estados().get(1).saldoPendiente());
    }

    @Test
    void obtenerEstadoEnviosDescartaSoloElObjetoMalformadoSinAbortarLosDemas() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(200,
            "[{\"status\":\"entregado\",\"updated_at\":\"2026-08-01T10:00:00Z\"},"
            + "{\"venta_local_id\":2,\"status\":\"pendiente\",\"updated_at\":\"2026-08-05T12:30:00Z\"}]");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();

        assertTrue(resultado.exitoso());
        assertEquals(1, resultado.estados().size());
        assertEquals("pendiente", resultado.estados().get(2).status());
    }

    @Test
    void obtenerEstadoEnviosParseaPendingBalanceComoStringNumerico() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        fake.respuestaEstadoEnvios = new PostgrestResponse(200,
            "[{\"venta_local_id\":1,\"status\":\"entregado\",\"updated_at\":\"2026-08-01T10:00:00Z\",\"pending_balance\":\"60.50\"}]");
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        SupabaseSyncService.ResultadoEstadoEnvios resultado = service.obtenerEstadoEnvios();

        assertTrue(resultado.exitoso());
        assertEquals(60.50, resultado.estados().get(1).saldoPendiente());
    }

    @Test
    void dividirObjetosNoSeDesincronizaPorLlavesDentroDeUnStringEscapado() {
        String json = "[{\"venta_local_id\":1,\"status\":\"entregado con \\\"comentario {con llaves}\\\"\","
            + "\"updated_at\":\"2026-08-01T10:00:00Z\"},{\"venta_local_id\":2,\"status\":\"pendiente\"}]";

        List<String> objetos = SupabaseSyncService.dividirObjetos(json);

        assertEquals(2, objetos.size());
        assertTrue(objetos.get(0).contains("\"venta_local_id\":1"));
        assertTrue(objetos.get(1).contains("\"venta_local_id\":2"));
    }

    @Test
    void sinVentasPendientesNoHaceLlamadaHttpDeVentas() throws Exception {
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, ventaDAO, fake);

        service.sincronizar();

        assertTrue(fake.llamadas.stream().noneMatch(l -> l.path().startsWith("/rest/v1/ventas")));
    }

    private void configurarSyncHabilitado() throws SQLException {
        Configuracion config = configDAO.obtenerConfiguracion();
        config.setSupabaseUrl("http://localhost:0");
        config.setSupabaseAnonKey("clave-anon");
        config.setSupabaseSyncEmail("sync@ferrematica.local");
        config.setSupabaseSyncPassword("clave-secreta");
        config.setSupabaseSyncHabilitado(true);
        config.setSupabaseSyncIntervaloMin(15);
        config.setSupabaseSyncVentasHabilitado(true);
        configDAO.guardarConfiguracion(config);
    }

    private int insertarItemConCodigoCrudo(String codigo, String nombre) throws SQLException {
        String sql = "INSERT INTO items (codigo, nombre, descripcion, precio_costo, precio_venta, stock, es_servicio, unidad, categoria) "
                + "VALUES (?, ?, 'desc', 1.0, 2.0, 1.0, 0, 'u', 'General')";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, codigo);
            pstmt.setString(2, nombre);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Extrae y des-escapa el valor de un campo string simple del JSON hand-rolled del servicio (solo para test). */
    private String extraerCampoString(String json, String campo) {
        String marca = "\"" + campo + "\":\"";
        int idx = json.indexOf(marca) + marca.length();
        StringBuilder sb = new StringBuilder();
        for (int i = idx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                char siguiente = json.charAt(++i);
                switch (siguiente) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(siguiente);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static class FakePostgrestClient implements PostgrestClient {
        final List<Llamada> llamadas = new ArrayList<>();
        PostgrestResponse respuestaAuth = new PostgrestResponse(200, "{\"access_token\":\"fake-jwt\"}");
        PostgrestResponse respuestaProducts = new PostgrestResponse(201, "[]");
        PostgrestResponse respuestaEstadoEnvios = new PostgrestResponse(200, "[]");

        record Llamada(String path, String jsonBody, Map<String, String> headers) {}

        @Override
        public PostgrestResponse post(String path, String jsonBody, Map<String, String> headers) {
            llamadas.add(new Llamada(path, jsonBody, new HashMap<>(headers)));
            if (path.startsWith("/auth/v1/token")) return respuestaAuth;
            if (path.startsWith("/rest/v1/rpc/estado_envios_pos")) return respuestaEstadoEnvios;
            return respuestaProducts;
        }

        Llamada buscarLlamadaProducts() {
            return llamadas.stream()
                .filter(l -> l.path().startsWith("/rest/v1/products"))
                .findFirst()
                .orElseThrow();
        }

        Llamada buscarLlamada(String prefijoPath) {
            return llamadas.stream()
                .filter(l -> l.path().startsWith(prefijoPath))
                .findFirst()
                .orElseThrow();
        }
    }
}
