package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ConexionDB;
import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import java.io.File;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketServiceTest {

    private final TicketService ticketService = new TicketService();

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

    private Venta ventaDePrueba(int id) {
        Venta venta = new Venta();
        venta.setId(id);
        venta.setFecha("2026-08-12 10:00");
        venta.setTotal(100.0);
        return venta;
    }

    @Test
    void generarTicketPDFUsaNombreDeArchivoDeterministaSinTimestamp(@TempDir File carpetaTickets) throws SQLException {
        ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();
        configuracionDAO.inicializarTabla();
        Configuracion config = configuracionDAO.obtenerConfiguracion();
        config.setRutaGuardadoTickets(carpetaTickets.getAbsolutePath());
        configuracionDAO.guardarConfiguracion(config);

        File ticket = ticketService.generarTicketPDF(ventaDePrueba(42));

        assertNotNull(ticket);
        assertEquals("Ticket_42.pdf", ticket.getName());
    }

    @Test
    void generarTicketPDFDosVecesPisaElMismoArchivoEnVezDeAcumular(@TempDir File carpetaTickets) throws SQLException {
        ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();
        configuracionDAO.inicializarTabla();
        Configuracion config = configuracionDAO.obtenerConfiguracion();
        config.setRutaGuardadoTickets(carpetaTickets.getAbsolutePath());
        configuracionDAO.guardarConfiguracion(config);

        File primero = ticketService.generarTicketPDF(ventaDePrueba(7));
        File segundo = ticketService.generarTicketPDF(ventaDePrueba(7));

        assertEquals(primero.getAbsolutePath(), segundo.getAbsolutePath());
        assertTrue(carpetaTickets.listFiles().length == 1);
    }
}
