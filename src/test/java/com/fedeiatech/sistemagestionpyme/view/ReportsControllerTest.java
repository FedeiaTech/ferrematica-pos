package com.fedeiatech.sistemagestionpyme.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReportsControllerTest {

    @Test
    void colorEstadoEnvioDevuelveElPastelAsignadoPorStatus() {
        assertEquals("#d4f4dd", ReportsController.colorEstadoEnvio("entregado"));
        assertEquals("#fff3cd", ReportsController.colorEstadoEnvio("en_camino"));
        assertEquals("#d6e9f8", ReportsController.colorEstadoEnvio("asignado"));
        assertEquals("#e8e8e8", ReportsController.colorEstadoEnvio("pendiente"));
        assertEquals("#f8d7da", ReportsController.colorEstadoEnvio("cancelado"));
    }

    @Test
    void colorEstadoEnvioDevuelveNullParaNoPedidoONullONoReconocido() {
        assertNull(ReportsController.colorEstadoEnvio(null));
        assertNull(ReportsController.colorEstadoEnvio("estado-inexistente"));
    }

    @Test
    void formatearEstadoEnvioCapitalizaPalabrasSeparadasPorGuionBajo() {
        assertEquals("En Camino", ReportsController.formatearEstadoEnvio("en_camino"));
        assertEquals("Entregado", ReportsController.formatearEstadoEnvio("entregado"));
        assertEquals("—", ReportsController.formatearEstadoEnvio(null));
    }

    @Test
    void formatearSaldoPendienteMuestraSegundaLineaCuandoHaySaldo() {
        assertEquals(String.format("falta $ %.2f", 60.00), ReportsController.formatearSaldoPendiente(60.00));
        assertEquals(String.format("falta $ %.2f", 0.50), ReportsController.formatearSaldoPendiente(0.50));
    }

    @Test
    void formatearSaldoPendienteEsNullCuandoNoHaySaldo() {
        assertNull(ReportsController.formatearSaldoPendiente(null));
    }

    @Test
    void formatearTooltipSaldoPendienteMuestraLineaCuandoHaySaldo() {
        assertEquals(String.format("Saldo pendiente: $ %.2f", 60.00), ReportsController.formatearTooltipSaldoPendiente(60.00));
    }

    @Test
    void formatearTooltipSaldoPendienteEsNullCuandoNoHaySaldo() {
        assertNull(ReportsController.formatearTooltipSaldoPendiente(null));
    }
}
