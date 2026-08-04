package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDAOTest {

    private final ItemDAO itemDAO = new ItemDAO();

    @BeforeEach
    void setUp(@TempDir File tempDir) throws SQLException {
        System.setProperty("db.path", new File(tempDir, "test.db").getAbsolutePath());
        ConexionDB.resetParaTests();
    }

    @AfterEach
    void tearDown() throws SQLException {
        ConexionDB.resetParaTests();
        System.clearProperty("db.path");
    }

    @Test
    void guardarYBuscarPorCodigo() throws SQLException {
        ItemVenta item = new ItemVenta(0, "COD-1", "Producto Test", "desc", 10.0, 20.0, 5.0, false);
        itemDAO.guardar(item);

        ItemVenta encontrado = itemDAO.buscarPorCodigo("COD-1");

        assertNotNull(encontrado);
        assertEquals("Producto Test", encontrado.getNombre());
        assertEquals(20.0, encontrado.getPrecioVenta());
        assertEquals(5.0, encontrado.getStock());
    }

    @Test
    void actualizarModificaLosCampos() throws SQLException {
        ItemVenta item = new ItemVenta(0, "COD-2", "Original", "desc", 10.0, 20.0, 5.0, false);
        itemDAO.guardar(item);
        ItemVenta guardado = itemDAO.buscarPorCodigo("COD-2");

        guardado.setNombre("Modificado");
        guardado.setPrecioVenta(30.0);
        itemDAO.actualizar(guardado);

        ItemVenta actualizado = itemDAO.buscarPorCodigo("COD-2");
        assertEquals("Modificado", actualizado.getNombre());
        assertEquals(30.0, actualizado.getPrecioVenta());
    }

    @Test
    void eliminarBorraElItem() throws SQLException {
        ItemVenta item = new ItemVenta(0, "COD-3", "Para borrar", "desc", 10.0, 20.0, 5.0, false);
        itemDAO.guardar(item);
        ItemVenta guardado = itemDAO.buscarPorCodigo("COD-3");

        itemDAO.eliminar(guardado.getId());

        assertNull(itemDAO.buscarPorCodigo("COD-3"));
    }

    @Test
    void validarCodigosParaSyncDetectaCodigosEnBlanco() throws SQLException {
        ItemVenta limpio = new ItemVenta(0, "COD-4", "Producto limpio", "desc", 10.0, 20.0, 5.0, false);
        itemDAO.guardar(limpio);
        insertarItemConCodigoCrudo("", "Sin codigo");
        insertarItemConCodigoCrudo("   ", "Codigo con espacios");

        List<String> offenders = itemDAO.validarCodigosParaSync();

        assertEquals(2, offenders.size());
        assertTrue(offenders.stream().allMatch(o -> o.contains("(vacío)")));
        assertTrue(offenders.stream().anyMatch(o -> o.contains("Sin codigo")));
        assertTrue(offenders.stream().anyMatch(o -> o.contains("Codigo con espacios")));
    }

    @Test
    void validarCodigosParaSyncVacioConDatosLimpios() throws SQLException {
        ItemVenta item = new ItemVenta(0, "COD-5", "Producto ok", "desc", 10.0, 20.0, 5.0, false);
        itemDAO.guardar(item);

        List<String> offenders = itemDAO.validarCodigosParaSync();

        assertTrue(offenders.isEmpty());
    }

    @Test
    void eliminarRegistraTombstoneYLimpiarLoQuita() throws SQLException {
        ItemVenta item = new ItemVenta(0, "COD-6", "Para tombstone", "desc", 10.0, 20.0, 5.0, false);
        itemDAO.guardar(item);
        ItemVenta guardado = itemDAO.buscarPorCodigo("COD-6");

        itemDAO.eliminar(guardado.getId());

        List<String> pendientes = itemDAO.listarCodigosEliminadosPendientes();
        assertTrue(pendientes.contains("COD-6"));

        itemDAO.limpiarEliminadosSincronizados(List.of("COD-6"));

        assertFalse(itemDAO.listarCodigosEliminadosPendientes().contains("COD-6"));
    }

    @Test
    void eliminarConCodigoEnBlancoNoRegistraTombstone() throws SQLException {
        int id = insertarItemConCodigoCrudo("", "Sin codigo para borrar");

        itemDAO.eliminar(id);

        assertFalse(itemDAO.listarCodigosEliminadosPendientes().contains(""));
    }

    @Test
    void idxItemsCodigoEsIdempotenteEntreDosCiclosDeConexion() throws SQLException {
        ConexionDB.getConexion();
        ConexionDB.resetParaTests();

        assertDoesNotThrow(ConexionDB::getConexion);
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
}
