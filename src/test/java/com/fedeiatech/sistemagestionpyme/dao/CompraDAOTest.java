package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Compra;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompraDAOTest {

    private final ItemDAO itemDAO = new ItemDAO();
    private final CompraDAO compraDAO = new CompraDAO();

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

    private Compra nuevaCompra(int idItem, double cantidad, double costoUnitario, String fecha) {
        Compra compra = new Compra();
        compra.setIdItem(idItem);
        compra.setCantidad(cantidad);
        compra.setCostoUnitario(costoUnitario);
        compra.calcularTotal();
        compra.setFecha(fecha);
        return compra;
    }

    @Test
    void registrarCompraSumaStockYPisaPrecioCosto() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-1", "Producto", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-1");

        Compra compra = nuevaCompra(item.getId(), 4.0, 8.0, "2026-08-01");
        compra.setProveedor("Acme");
        compraDAO.registrarCompra(compra);

        ItemVenta actualizado = itemDAO.buscarPorCodigo("COD-1");
        assertEquals(14.0, actualizado.getStock());
        assertEquals(8.0, actualizado.getPrecioCosto());
        assertTrue(compra.getId() > 0);
    }

    @Test
    void registrarCompraHaceRollbackSiElItemNoExiste() throws SQLException {
        Compra compra = nuevaCompra(9999, 5.0, 10.0, "2026-08-01");

        assertThrows(SQLException.class, () -> compraDAO.registrarCompra(compra));

        assertEquals(0, contarCompras());
    }

    @Test
    void registrarCompraRechazaItemDeServicio() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "SERV-1", "Servicio", "desc", 0.0, 50.0, 0.0, true));
        ItemVenta servicio = itemDAO.buscarPorCodigo("SERV-1");

        Compra compra = nuevaCompra(servicio.getId(), 1.0, 10.0, "2026-08-01");

        assertThrows(SQLException.class, () -> compraDAO.registrarCompra(compra));

        assertEquals(0, contarCompras());
    }

    @Test
    void sumarComprasEntreDevuelveSoloElRango() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-2", "Producto 2", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-2");

        compraDAO.registrarCompra(nuevaCompra(item.getId(), 1.0, 100.0, "2026-07-10"));
        compraDAO.registrarCompra(nuevaCompra(item.getId(), 1.0, 200.0, "2026-07-15"));
        compraDAO.registrarCompra(nuevaCompra(item.getId(), 1.0, 300.0, "2026-08-01"));

        double total = compraDAO.sumarComprasEntre("2026-07-01", "2026-07-31");

        assertEquals(300.0, total);
    }

    @Test
    void obtenerComprasPorMesAgrupaPorMes() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-3", "Producto 3", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-3");

        compraDAO.registrarCompra(nuevaCompra(item.getId(), 1.0, 100.0, "2026-07-10"));
        compraDAO.registrarCompra(nuevaCompra(item.getId(), 1.0, 50.0, "2026-07-20"));

        List<String[]> meses = compraDAO.obtenerComprasPorMes();

        assertEquals(1, meses.size());
        assertEquals("2026-07", meses.get(0)[0]);
        assertEquals("2", meses.get(0)[1]);
        assertEquals(150.0, Double.parseDouble(meses.get(0)[2]));
    }

    @Test
    void listarHistoricoSobreviveAlBorradoDelItem() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-4", "Producto 4", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-4");

        compraDAO.registrarCompra(nuevaCompra(item.getId(), 1.0, 100.0, "2026-08-01"));

        itemDAO.eliminar(item.getId());

        List<Compra> historico = compraDAO.listarHistorico(50);

        assertEquals(1, historico.size());
        assertEquals("(producto eliminado)", historico.get(0).getNombreItem());
    }

    private int contarCompras() throws SQLException {
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM compras")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
