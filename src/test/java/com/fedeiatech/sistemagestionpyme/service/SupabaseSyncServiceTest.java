package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ComboDAO;
import com.fedeiatech.sistemagestionpyme.dao.ConexionDB;
import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.model.Combo;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    void noIncluyeCombosEnElSnapshot() throws Exception {
        itemDAO.guardar(new ItemVenta(0, "SKU-2", "Tornillo", "d", 1.0, 2.0, 100.0, false));
        Combo combo = new Combo("COMBO-1", "Kit de arranque", "d", 500.0);
        new ComboDAO().guardar(combo);
        configurarSyncHabilitado();
        FakePostgrestClient fake = new FakePostgrestClient();
        SupabaseSyncService service = new SupabaseSyncService(itemDAO, configDAO, fake);

        service.sincronizar();

        String body = fake.buscarLlamadaProducts().jsonBody();
        assertEquals(1, itemDAO.listarTodos().size());
        assertTrue(body.contains("SKU-2"));
        assertFalse(body.contains("COMBO-1"));
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

    private void configurarSyncHabilitado() throws SQLException {
        Configuracion config = configDAO.obtenerConfiguracion();
        config.setSupabaseUrl("http://localhost:0");
        config.setSupabaseAnonKey("clave-anon");
        config.setSupabaseSyncEmail("sync@ferrematica.local");
        config.setSupabaseSyncPassword("clave-secreta");
        config.setSupabaseSyncHabilitado(true);
        config.setSupabaseSyncIntervaloMin(15);
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

        record Llamada(String path, String jsonBody, Map<String, String> headers) {}

        @Override
        public PostgrestResponse post(String path, String jsonBody, Map<String, String> headers) {
            llamadas.add(new Llamada(path, jsonBody, new HashMap<>(headers)));
            if (path.startsWith("/auth/v1/token")) return respuestaAuth;
            return respuestaProducts;
        }

        Llamada buscarLlamadaProducts() {
            return llamadas.stream()
                .filter(l -> l.path().startsWith("/rest/v1/products"))
                .findFirst()
                .orElseThrow();
        }
    }
}
