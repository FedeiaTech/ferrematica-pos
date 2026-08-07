package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import java.io.File;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VentaDAOTest {

    private final ItemDAO itemDAO = new ItemDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

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
    void registrarVentaDescuentaStockDelItem() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-1", "Producto", "desc", 10.0, 20.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-1");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28");
        venta.agregarDetalle(new DetalleVenta(item, 3.0));

        ventaDAO.registrarVenta(venta);

        ItemVenta actualizado = itemDAO.buscarPorCodigo("COD-1");
        assertEquals(7.0, actualizado.getStock());
        assertTrue(venta.getId() > 0);
    }

    @Test
    void registrarVentaNoDescuentaStockDeServicios() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "SERV-1", "Servicio", "desc", 0.0, 50.0, 0.0, true));
        ItemVenta servicio = itemDAO.buscarPorCodigo("SERV-1");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28");
        venta.agregarDetalle(new DetalleVenta(servicio, 1.0));

        ventaDAO.registrarVenta(venta);

        ItemVenta actualizado = itemDAO.buscarPorCodigo("SERV-1");
        assertEquals(0.0, actualizado.getStock());
    }

    @Test
    void obtenerVentasPorMesNoInflaElTotalConVentasDeVariosItems() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-1", "Producto 1", "desc", 5.0, 10.0, 100.0, false));
        itemDAO.guardar(new ItemVenta(0, "COD-2", "Producto 2", "desc", 5.0, 20.0, 100.0, false));
        ItemVenta item1 = itemDAO.buscarPorCodigo("COD-1");
        ItemVenta item2 = itemDAO.buscarPorCodigo("COD-2");

        // Un solo ticket con 2 líneas de producto distintas: total real = 10 + 20 = 30.
        Venta venta = new Venta();
        venta.setFecha("2026-07-28");
        venta.agregarDetalle(new DetalleVenta(item1, 1.0));
        venta.agregarDetalle(new DetalleVenta(item2, 1.0));
        ventaDAO.registrarVenta(venta);

        java.util.List<String[]> meses = ventaDAO.obtenerVentasPorMes();

        assertEquals(1, meses.size());
        assertEquals("1", meses.get(0)[1]);
        assertEquals(30.0, Double.parseDouble(meses.get(0)[2]));
    }

    @Test
    void sumarVentasEntreDevuelveSoloElRango() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-1", "Producto", "desc", 5.0, 10.0, 100.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-1");

        Venta dentro1 = new Venta();
        dentro1.setFecha("2026-07-10");
        dentro1.agregarDetalle(new DetalleVenta(item, 1.0));
        ventaDAO.registrarVenta(dentro1);

        Venta dentro2 = new Venta();
        dentro2.setFecha("2026-07-15");
        dentro2.agregarDetalle(new DetalleVenta(item, 2.0));
        ventaDAO.registrarVenta(dentro2);

        Venta fuera = new Venta();
        fuera.setFecha("2026-08-01");
        fuera.agregarDetalle(new DetalleVenta(item, 1.0));
        ventaDAO.registrarVenta(fuera);

        double total = ventaDAO.sumarVentasEntre("2026-07-01", "2026-07-31");

        assertEquals(30.0, total);
    }
}
