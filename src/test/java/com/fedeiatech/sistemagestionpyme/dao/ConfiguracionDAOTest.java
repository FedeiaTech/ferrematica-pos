package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import java.io.File;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfiguracionDAOTest {

    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();

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
    void inicializarTablaEsIdempotente() {
        configuracionDAO.inicializarTabla();
        assertDoesNotThrow(configuracionDAO::inicializarTabla);
    }

    @Test
    void guardarYObtenerConfiguracionPreservaValores() throws SQLException {
        configuracionDAO.inicializarTabla();

        Configuracion config = configuracionDAO.obtenerConfiguracion();
        assertNotNull(config);
        config.setNombreEmpresa("Ferretería Don José");
        config.setRecargoTarjeta(8.5);
        configuracionDAO.guardarConfiguracion(config);

        Configuracion recargado = configuracionDAO.obtenerConfiguracion();
        assertEquals("Ferretería Don José", recargado.getNombreEmpresa());
        assertEquals(8.5, recargado.getRecargoTarjeta());
    }

    @Test
    void datosDeSyncSupabasePersistenEntreReinicios() throws SQLException {
        configuracionDAO.inicializarTabla();

        Configuracion config = configuracionDAO.obtenerConfiguracion();
        assertNotNull(config);
        config.setSupabaseUrl("https://proyecto.supabase.co");
        config.setSupabaseAnonKey("anon-key-123");
        config.setSupabaseSyncHabilitado(true);
        config.setSupabaseSyncIntervaloMin(30);
        config.setSupabaseSyncEmail("dueno@ferreteria.com");
        config.setSupabaseSyncPassword("secreta123");
        configuracionDAO.guardarConfiguracion(config);

        // Simula un reinicio: nueva instancia del DAO contra la misma conexión/archivo
        ConfiguracionDAO reiniciado = new ConfiguracionDAO();
        Configuracion recargado = reiniciado.obtenerConfiguracion();

        assertEquals("https://proyecto.supabase.co", recargado.getSupabaseUrl());
        assertEquals("anon-key-123", recargado.getSupabaseAnonKey());
        assertEquals(true, recargado.isSupabaseSyncHabilitado());
        assertEquals(30, recargado.getSupabaseSyncIntervaloMin());
        assertEquals("dueno@ferreteria.com", recargado.getSupabaseSyncEmail());
        assertEquals("secreta123", recargado.getSupabaseSyncPassword());
    }

    @Test
    void ultimaSincronizacionExitosaPersisteYSobrevivReinicioSinPisarOtrosCampos() throws SQLException {
        configuracionDAO.inicializarTabla();

        Configuracion config = configuracionDAO.obtenerConfiguracion();
        config.setNombreEmpresa("Ferretería Don José");
        configuracionDAO.guardarConfiguracion(config);

        String instante = "2026-08-04T18:00:00Z";
        configuracionDAO.actualizarUltimaSincronizacionExitosa(instante);

        ConfiguracionDAO reiniciado = new ConfiguracionDAO();
        Configuracion recargado = reiniciado.obtenerConfiguracion();

        assertEquals(instante, recargado.getSupabaseUltimaSyncExitosa());
        assertEquals("Ferretería Don José", recargado.getNombreEmpresa());
    }

    @Test
    void obtenerOGenerarInstallIdGeneraUnaSolaVezYLoReutiliza() throws SQLException {
        configuracionDAO.inicializarTabla();

        String primeraLlamada = configuracionDAO.obtenerOGenerarInstallId();
        assertNotNull(primeraLlamada);
        assertDoesNotThrow(() -> java.util.UUID.fromString(primeraLlamada));

        String segundaLlamada = configuracionDAO.obtenerOGenerarInstallId();
        assertEquals(primeraLlamada, segundaLlamada);

        // Simula un reinicio: nueva instancia del DAO debe seguir viendo el mismo install_id persistido.
        ConfiguracionDAO reiniciado = new ConfiguracionDAO();
        assertEquals(primeraLlamada, reiniciado.obtenerOGenerarInstallId());
    }
}
