package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.io.File;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
