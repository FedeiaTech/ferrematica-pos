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
        config.setMargenGananciaPct(35.5);
        configuracionDAO.guardarConfiguracion(config);

        Configuracion recargado = configuracionDAO.obtenerConfiguracion();
        assertEquals("Ferretería Don José", recargado.getNombreEmpresa());
        assertEquals(35.5, recargado.getMargenGananciaPct());
    }
}
