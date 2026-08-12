package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Combo;
import com.fedeiatech.sistemagestionpyme.model.ComponenteCombo;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VentaDAOTest {

    private final ItemDAO itemDAO = new ItemDAO();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final ComboDAO comboDAO = new ComboDAO();

    @BeforeEach
    void setUp(@TempDir File tempDir) throws SQLException {
        System.setProperty("db.path", new File(tempDir, "test.db").getAbsolutePath());
        ConexionDB.resetParaTests();
        SessionService.getInstance().iniciarSesion(new Usuario("admin-test", "hash", Usuario.Rol.ADMIN));
    }

    @AfterEach
    void tearDown() throws SQLException {
        ConexionDB.resetParaTests();
        System.clearProperty("db.path");
        SessionService.getInstance().cerrarSesion();
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
    void obtenerGananciaEstimadaDelDiaRestaLosGastosDelDia() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-GAN", "Producto Ganancia", "desc", 5.0, 10.0, 100.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-GAN");

        String hoy = java.time.LocalDate.now().toString();
        String fechaHoraHoy = hoy + "T10:00:00";

        Venta venta = new Venta();
        venta.setFecha(fechaHoraHoy);
        venta.agregarDetalle(new DetalleVenta(item, 2.0));
        ventaDAO.registrarVenta(venta);

        double gananciaSinGastos = ventaDAO.obtenerGananciaEstimadaDelDia();
        assertEquals(10.0, gananciaSinGastos);

        GastoDAO gastoDAO = new GastoDAO();
        com.fedeiatech.sistemagestionpyme.model.Gasto gasto = new com.fedeiatech.sistemagestionpyme.model.Gasto();
        gasto.setConcepto("Nafta");
        gasto.setMonto(4.0);
        gasto.setFecha(hoy);
        gastoDAO.registrar(gasto);

        double gananciaConGastos = ventaDAO.obtenerGananciaEstimadaDelDia();

        assertEquals(6.0, gananciaConGastos);
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

    @Test
    void listarVentasEntreDevuelveDesgloseYExcluyeAnuladas() throws SQLException {
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

        Venta anulada = new Venta();
        anulada.setFecha("2026-07-20");
        anulada.agregarDetalle(new DetalleVenta(item, 1.0));
        ventaDAO.registrarVenta(anulada);
        ventaDAO.anularVenta(anulada.getId(), "Prueba de desglose");

        Venta fuera = new Venta();
        fuera.setFecha("2026-08-01");
        fuera.agregarDetalle(new DetalleVenta(item, 1.0));
        ventaDAO.registrarVenta(fuera);

        java.util.List<Venta> detalle = ventaDAO.listarVentasEntre("2026-07-01", "2026-07-31");

        assertEquals(2, detalle.size(), "Debe listar solo las ventas activas dentro del rango, sin la anulada ni la fuera de rango");
        assertTrue(detalle.stream().anyMatch(v -> v.getId() == dentro1.getId()));
        assertTrue(detalle.stream().anyMatch(v -> v.getId() == dentro2.getId()));
        assertTrue(detalle.stream().noneMatch(v -> v.getId() == anulada.getId()));
        assertTrue(detalle.stream().noneMatch(v -> v.getId() == fuera.getId()));
    }

    @Test
    void anularVentaSimpleReponeStockYMarcaEstado() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-1", "Producto", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-1");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28");
        venta.agregarDetalle(new DetalleVenta(item, 3.0));
        ventaDAO.registrarVenta(venta);

        ItemVenta antesDeAnular = itemDAO.buscarPorCodigo("COD-1");
        assertEquals(7.0, antesDeAnular.getStock());

        boolean anulada = ventaDAO.anularVenta(venta.getId(), "Cliente se arrepintió");
        assertTrue(anulada);

        ItemVenta despuesDeAnular = itemDAO.buscarPorCodigo("COD-1");
        assertEquals(10.0, despuesDeAnular.getStock());

        Venta ventaAnulada = ventaDAO.obtenerVentaCompleta(venta.getId());
        assertTrue(ventaAnulada.estaAnulada());
        assertEquals("Cliente se arrepintió", ventaAnulada.getMotivoAnulacion());
    }

    @Test
    void anularVentaRechazaSiElUsuarioActivoNoEsAdmin() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-CAJERO", "Producto", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-CAJERO");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28");
        venta.agregarDetalle(new DetalleVenta(item, 3.0));
        ventaDAO.registrarVenta(venta);

        SessionService.getInstance().iniciarSesion(new Usuario("cajero-test", "hash", Usuario.Rol.CAJERO));
        try {
            boolean anulada = ventaDAO.anularVenta(venta.getId(), "Intento no autorizado");
            assertFalse(anulada, "Un CAJERO no debe poder anular una venta, aunque invoque el DAO directamente");
        } finally {
            SessionService.getInstance().iniciarSesion(new Usuario("admin-test", "hash", Usuario.Rol.ADMIN));
        }

        ItemVenta itemSinCambios = itemDAO.buscarPorCodigo("COD-CAJERO");
        assertEquals(7.0, itemSinCambios.getStock(), "El stock no debe reponerse si la anulación fue rechazada");

        Venta ventaSinAnular = ventaDAO.obtenerVentaCompleta(venta.getId());
        assertFalse(ventaSinAnular.estaAnulada());
    }

    @Test
    void anularVentaConComboReponeStockDeCadaComponente() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-A", "Componente A", "desc", 2.0, 5.0, 20.0, false));
        itemDAO.guardar(new ItemVenta(0, "COD-B", "Componente B", "desc", 3.0, 8.0, 20.0, false));
        ItemVenta compA = itemDAO.buscarPorCodigo("COD-A");
        ItemVenta compB = itemDAO.buscarPorCodigo("COD-B");

        Combo combo = new Combo("COMBO-1", "Combo Test", "desc", 15.0);
        combo.agregarComponente(new ComponenteCombo(compA.getId(), compA.getCodigo(), compA.getNombre(), 2.0));
        combo.agregarComponente(new ComponenteCombo(compB.getId(), compB.getCodigo(), compB.getNombre(), 1.0));
        comboDAO.guardar(combo);

        Venta venta = new Venta();
        venta.setFecha("2026-07-28");
        venta.agregarDetalle(new DetalleVenta(combo, 3.0));
        ventaDAO.registrarVenta(venta);

        assertEquals(14.0, itemDAO.buscarPorCodigo("COD-A").getStock());
        assertEquals(17.0, itemDAO.buscarPorCodigo("COD-B").getStock());

        boolean anulada = ventaDAO.anularVenta(venta.getId(), "Error de carga");
        assertTrue(anulada);

        assertEquals(20.0, itemDAO.buscarPorCodigo("COD-A").getStock());
        assertEquals(20.0, itemDAO.buscarPorCodigo("COD-B").getStock());
    }

    @Test
    void dobleAnulacionDevuelveFalseYNoDuplicaStock() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-1", "Producto", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-1");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28");
        venta.agregarDetalle(new DetalleVenta(item, 3.0));
        ventaDAO.registrarVenta(venta);

        assertTrue(ventaDAO.anularVenta(venta.getId(), "Primer motivo"));
        assertEquals(10.0, itemDAO.buscarPorCodigo("COD-1").getStock());

        boolean segundaAnulacion = ventaDAO.anularVenta(venta.getId(), "Segundo motivo");
        assertFalse(segundaAnulacion);
        assertEquals(10.0, itemDAO.buscarPorCodigo("COD-1").getStock());

        Venta ventaAnulada = ventaDAO.obtenerVentaCompleta(venta.getId());
        assertEquals("Primer motivo", ventaAnulada.getMotivoAnulacion());
    }

    @Test
    void anularVentaConItemBorradoNoAbortaLaTransaccion() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-1", "Producto a borrar", "desc", 5.0, 10.0, 10.0, false));
        itemDAO.guardar(new ItemVenta(0, "COD-2", "Producto que sobrevive", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta itemBorrado = itemDAO.buscarPorCodigo("COD-1");
        ItemVenta itemSobreviviente = itemDAO.buscarPorCodigo("COD-2");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28");
        venta.agregarDetalle(new DetalleVenta(itemBorrado, 2.0));
        venta.agregarDetalle(new DetalleVenta(itemSobreviviente, 1.0));
        ventaDAO.registrarVenta(venta);

        itemDAO.eliminar(itemBorrado.getId());

        boolean anulada = ventaDAO.anularVenta(venta.getId(), "Item ya no existe");
        assertTrue(anulada);
        assertEquals(10.0, itemDAO.buscarPorCodigo("COD-2").getStock());
    }

    @Test
    void barridoDeAgregacionesExcluyeVentaAnuladaYHistoricoLaConserva() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "PROD-A", "Producto A", "desc", 5.0, 10.0, 100.0, false));
        itemDAO.guardar(new ItemVenta(0, "PROD-B", "Producto B", "desc", 10.0, 20.0, 100.0, false));
        ItemVenta itemA = itemDAO.buscarPorCodigo("PROD-A");
        ItemVenta itemB = itemDAO.buscarPorCodigo("PROD-B");

        String hoy = java.time.LocalDate.now().toString();
        String fechaHoraHoy = hoy + "T12:00:00";

        Venta venta1 = new Venta();
        venta1.setFecha(fechaHoraHoy);
        venta1.agregarDetalle(new DetalleVenta(itemA, 2.0));
        venta1.agregarDetalle(new DetalleVenta(itemB, 1.0));
        ventaDAO.registrarVenta(venta1);

        Venta venta2 = new Venta();
        venta2.setFecha(fechaHoraHoy);
        venta2.agregarDetalle(new DetalleVenta(itemA, 1.0));
        venta2.agregarDetalle(new DetalleVenta(itemB, 1.0));
        ventaDAO.registrarVenta(venta2);

        Venta venta3 = new Venta();
        venta3.setFecha(fechaHoraHoy);
        venta3.agregarDetalle(new DetalleVenta(itemA, 1.0));
        ventaDAO.registrarVenta(venta3);

        double sumaDelDiaAntes = ventaDAO.sumarVentasDelDia();
        double sumaEntreAntes = ventaDAO.sumarVentasEntre(hoy, hoy);
        int contarDelDiaAntes = ventaDAO.contarVentasDelDia();
        double gananciaAntes = ventaDAO.obtenerGananciaEstimadaDelDia();
        double ventasUltimos7DiasAntes = ventaDAO.obtenerVentasUltimos7Dias().getOrDefault(hoy, 0.0);
        double top5Antes = ventaDAO.obtenerTop5ProductosMasVendidos().getOrDefault("Producto A", 0.0);
        int resumenDiarioCantAntes = Integer.parseInt(ventaDAO.obtenerResumenDiario(hoy, hoy).get(0)[1]);
        int detalleCompletoFilasAntes = ventaDAO.obtenerDetalleCompleto(hoy, hoy).size();
        int porSemanaCantAntes = Integer.parseInt(ultimaFila(ventaDAO.obtenerVentasPorSemana())[1]);
        int porMesCantAntes = Integer.parseInt(ultimaFila(ventaDAO.obtenerVentasPorMes())[1]);
        boolean marketBasketTienePar = ventaDAO.obtenerMarketBasket().stream()
                .anyMatch(fila -> fila[2].equals("2"));
        assertTrue(marketBasketTienePar, "Antes de anular, el par A-B debe tener frecuencia 2");
        double ventasPorProductoAAntes = ventaDAO.obtenerVentasPorProducto(hoy, hoy).stream()
                .filter(fila -> fila[0].equals("Producto A"))
                .mapToDouble(fila -> Double.parseDouble(fila[2]))
                .findFirst().orElse(0.0);
        double totalesPorHoraAntes = ventaDAO.obtenerTotalesPorHora().values().stream()
                .mapToDouble(Double::doubleValue).sum();
        double heatmapCantAntes = ventaDAO.obtenerHeatmapDiaHora().values().stream()
                .mapToDouble(Double::doubleValue).sum();

        boolean anulada = ventaDAO.anularVenta(venta2.getId(), "Prueba de barrido");
        assertTrue(anulada);

        assertTrue(ventaDAO.sumarVentasDelDia() < sumaDelDiaAntes);
        assertTrue(ventaDAO.sumarVentasEntre(hoy, hoy) < sumaEntreAntes);
        assertEquals(contarDelDiaAntes - 1, ventaDAO.contarVentasDelDia());
        assertTrue(ventaDAO.obtenerGananciaEstimadaDelDia() < gananciaAntes);
        assertTrue(ventaDAO.obtenerVentasUltimos7Dias().getOrDefault(hoy, 0.0) < ventasUltimos7DiasAntes);
        double top5Despues = ventaDAO.obtenerTop5ProductosMasVendidos().getOrDefault("Producto A", 0.0);
        assertTrue(top5Despues < top5Antes);
        assertTrue(Integer.parseInt(ventaDAO.obtenerResumenDiario(hoy, hoy).get(0)[1]) < resumenDiarioCantAntes);
        assertTrue(ventaDAO.obtenerDetalleCompleto(hoy, hoy).size() < detalleCompletoFilasAntes);
        assertTrue(Integer.parseInt(ultimaFila(ventaDAO.obtenerVentasPorSemana())[1]) < porSemanaCantAntes);
        assertTrue(Integer.parseInt(ultimaFila(ventaDAO.obtenerVentasPorMes())[1]) < porMesCantAntes);
        assertFalse(ventaDAO.obtenerMarketBasket().stream().anyMatch(fila -> fila[2].equals("2")),
                "Tras anular, el par A-B ya no debe alcanzar frecuencia 2");
        double ventasPorProductoADespues = ventaDAO.obtenerVentasPorProducto(hoy, hoy).stream()
                .filter(fila -> fila[0].equals("Producto A"))
                .mapToDouble(fila -> Double.parseDouble(fila[2]))
                .findFirst().orElse(0.0);
        assertTrue(ventasPorProductoADespues < ventasPorProductoAAntes,
                "obtenerVentasPorProducto debe excluir la cantidad vendida en la venta anulada");
        assertTrue(ventaDAO.obtenerTotalesPorHora().values().stream().mapToDouble(Double::doubleValue).sum() < totalesPorHoraAntes,
                "obtenerTotalesPorHora debe excluir el total de la venta anulada");
        assertTrue(ventaDAO.obtenerHeatmapDiaHora().values().stream().mapToDouble(Double::doubleValue).sum() < heatmapCantAntes,
                "obtenerHeatmapDiaHora debe excluir la venta anulada del conteo");

        java.util.List<Venta> historico = ventaDAO.listarVentasHistoricas();
        assertEquals(3, historico.size(), "listarVentasHistoricas debe seguir mostrando la venta anulada");
        assertTrue(historico.stream().anyMatch(v -> v.getId() == venta2.getId() && v.estaAnulada()));

        Venta ventaCompletaAnulada = ventaDAO.obtenerVentaCompleta(venta2.getId());
        assertTrue(ventaCompletaAnulada.estaAnulada());
    }

    private static String[] ultimaFila(java.util.List<String[]> filas) {
        return filas.get(filas.size() - 1);
    }

    @Test
    void ventaNuevaApareceComoPendienteDeSyncConSusDetalles() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-SYNC-1", "Producto Sync", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-SYNC-1");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28T10:00:00");
        venta.agregarDetalle(new DetalleVenta(item, 3.0));
        ventaDAO.registrarVenta(venta);

        java.util.List<VentaDAO.VentaPendiente> pendientes = ventaDAO.listarVentasPendientesDeSync();

        assertEquals(1, pendientes.size());
        VentaDAO.VentaPendiente pendiente = pendientes.get(0);
        assertEquals(venta.getId(), pendiente.id());
        assertEquals(1, pendiente.detalles().size());
        assertEquals("COD-SYNC-1", pendiente.detalles().get(0).productoCodigo());
        assertEquals(3.0, pendiente.detalles().get(0).cantidad());
    }

    @Test
    void marcarVentasSincronizadasLasSacaDeLaListaDePendientes() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-SYNC-2", "Producto Sync 2", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-SYNC-2");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28T10:00:00");
        venta.agregarDetalle(new DetalleVenta(item, 1.0));
        ventaDAO.registrarVenta(venta);

        ventaDAO.marcarVentasSincronizadas(java.util.List.of(venta.getId()));

        assertTrue(ventaDAO.listarVentasPendientesDeSync().isEmpty());
    }

    @Test
    void anularVentaVuelveAMarcarLaVentaComoPendienteDeSync() throws SQLException {
        itemDAO.guardar(new ItemVenta(0, "COD-SYNC-3", "Producto Sync 3", "desc", 5.0, 10.0, 10.0, false));
        ItemVenta item = itemDAO.buscarPorCodigo("COD-SYNC-3");

        Venta venta = new Venta();
        venta.setFecha("2026-07-28T10:00:00");
        venta.agregarDetalle(new DetalleVenta(item, 1.0));
        ventaDAO.registrarVenta(venta);

        ventaDAO.marcarVentasSincronizadas(java.util.List.of(venta.getId()));
        assertTrue(ventaDAO.listarVentasPendientesDeSync().isEmpty());

        boolean anulada = ventaDAO.anularVenta(venta.getId(), "Se re-empuja tras anular");
        assertTrue(anulada);

        java.util.List<VentaDAO.VentaPendiente> pendientesTrasAnular = ventaDAO.listarVentasPendientesDeSync();
        assertEquals(1, pendientesTrasAnular.size(), "anularVenta debe limpiar sincronizada_en para forzar el re-push");
        assertEquals(venta.getId(), pendientesTrasAnular.get(0).id());
    }

    @Test
    void migracionEstadoEsIdempotenteYNormalizaFilasNull() throws SQLException {
        Connection conn = ConexionDB.getConexion();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO ventas (fecha, total, estado) VALUES ('2026-01-01T10:00:00', 15.0, NULL)");
        }

        ConexionDB.resetParaTests();
        Connection reconectada = ConexionDB.getConexion();
        assertNotNull(reconectada);

        java.util.List<Venta> historico = ventaDAO.listarVentasHistoricas();
        Venta filaPrevia = historico.stream().filter(v -> v.getTotal() == 15.0).findFirst().orElseThrow();

        assertEquals(Venta.ESTADO_COMPLETADA, filaPrevia.getEstado());
        assertFalse(filaPrevia.estaAnulada());
    }
}
