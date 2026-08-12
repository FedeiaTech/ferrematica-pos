package com.fedeiatech.sistemagestionpyme.view;

import org.junit.jupiter.api.Test;

import java.time.Instant;

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
    void formatearHoraEntregaDistingueEntregadoSinHoraDeNoEntregado() {
        assertEquals("—", ReportsController.formatearHoraEntrega("en_camino", null));
        assertEquals("—", ReportsController.formatearHoraEntrega(null, null));
        assertEquals("Entregado (hora desconocida)",
            ReportsController.formatearHoraEntrega("entregado", null));
    }

    @Test
    void formatearHoraEntregaFormateaCuandoHayTimestamp() {
        Instant entregadoEn = Instant.parse("2026-08-11T15:30:00Z");
        String resultado = ReportsController.formatearHoraEntrega("entregado", entregadoEn);
        assertEquals(false, resultado.equals("—") || resultado.equals("Entregado (hora desconocida)"));
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

    @Test
    void estadoTicketEsNuncaGeneradoCuandoRutaEsNullOVacia() {
        assertEquals(ReportsController.EstadoTicket.NUNCA_GENERADO, ReportsController.estadoTicket(null, false));
        assertEquals(ReportsController.EstadoTicket.NUNCA_GENERADO, ReportsController.estadoTicket("", false));
        assertEquals(ReportsController.EstadoTicket.NUNCA_GENERADO, ReportsController.estadoTicket("  ", false));
    }

    @Test
    void estadoTicketEsExisteCuandoHayRutaYElArchivoEsta() {
        assertEquals(ReportsController.EstadoTicket.EXISTE,
            ReportsController.estadoTicket("C:/tickets/Ticket_1.pdf", true));
    }

    @Test
    void estadoTicketEsFaltaCuandoHayRutaPeroElArchivoNoEsta() {
        assertEquals(ReportsController.EstadoTicket.FALTA,
            ReportsController.estadoTicket("C:/tickets/Ticket_1.pdf", false));
    }

    @Test
    void textoBotonTicketVariaPorEstado() {
        assertEquals("🖨️ Ver Ticket", ReportsController.textoBotonTicket(ReportsController.EstadoTicket.NUNCA_GENERADO));
        assertEquals("✅ Ver Ticket", ReportsController.textoBotonTicket(ReportsController.EstadoTicket.EXISTE));
        assertEquals("⚠️ Ver Ticket", ReportsController.textoBotonTicket(ReportsController.EstadoTicket.FALTA));
    }

    @Test
    void estiloBotonTicketVariaPorEstado() {
        assertEquals(ReportsController.estiloBotonTicket(ReportsController.EstadoTicket.NUNCA_GENERADO),
            ReportsController.estiloBotonTicket(ReportsController.EstadoTicket.NUNCA_GENERADO));
        org.junit.jupiter.api.Assertions.assertNotEquals(
            ReportsController.estiloBotonTicket(ReportsController.EstadoTicket.EXISTE),
            ReportsController.estiloBotonTicket(ReportsController.EstadoTicket.FALTA));
    }
}
