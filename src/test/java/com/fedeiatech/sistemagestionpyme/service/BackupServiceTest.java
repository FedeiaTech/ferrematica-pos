package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ConexionDB;
import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.UsuarioDAO;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupServiceTest {

    private File dbFile;

    @BeforeEach
    void setUp(@TempDir File tempDir) throws SQLException {
        dbFile = new File(tempDir, "gestion_pyme.db");
        System.setProperty("db.path", dbFile.getAbsolutePath());
        ConexionDB.resetParaTests();

        new ConfiguracionDAO().inicializarTabla();
        new UsuarioDAO().inicializarTabla();
        try (Connection conn = ConexionDB.getConexion(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO items (codigo, nombre, precio_costo, precio_venta, stock, es_servicio) "
                + "VALUES ('COD-1', 'Producto Test', 10.0, 20.0, 5.0, 0)");
            stmt.execute("INSERT INTO ventas (fecha, total, estado) VALUES ('2026-08-15', 20.0, 'completada')");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        ConexionDB.resetParaTests();
        System.clearProperty("db.path");
    }

    @Test
    void backupCompletoCopiaElArchivoTalCual() throws Exception {
        File destino = new File(dbFile.getParentFile(), "backup_completo.db");

        BackupService.backupCompleto(destino);

        assertTrue(BackupService.esBackupCompleto(destino));
        assertEquals(1, contarFilas(destino, "items"));
    }

    @Test
    void backupSoloDatosExcluyeConfiguracionYUsuarios() throws Exception {
        File destino = new File(dbFile.getParentFile(), "backup_datos.db");

        BackupService.backupSoloDatos(destino);

        assertFalse(BackupService.esBackupCompleto(destino));
        assertEquals(1, contarFilas(destino, "items"));
        assertEquals(1, contarFilas(destino, "ventas"));
    }

    @Test
    void restaurarSoloDatosNoTocaConfiguracionNiUsuarios() throws Exception {
        File backupDatos = new File(dbFile.getParentFile(), "backup_datos.db");
        BackupService.backupSoloDatos(backupDatos);

        // Cambia el estado "vivo" para verificar que sobrevive a la restauración de solo-datos.
        try (Connection conn = ConexionDB.getConexion(); Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE configuracion SET nombre_empresa = 'Empresa Viva' WHERE id = 1");
            stmt.execute("DELETE FROM items");
            stmt.execute("INSERT INTO items (codigo, nombre, precio_costo, precio_venta, stock, es_servicio) "
                + "VALUES ('COD-2', 'Otro Producto', 1.0, 2.0, 1.0, 0)");
        }

        BackupService.restaurarSoloDatos(backupDatos);

        try (Connection conn = ConexionDB.getConexion(); Statement stmt = conn.createStatement()) {
            var rsConfig = stmt.executeQuery("SELECT nombre_empresa FROM configuracion WHERE id = 1");
            rsConfig.next();
            assertEquals("Empresa Viva", rsConfig.getString(1));

            var rsItems = stmt.executeQuery("SELECT codigo FROM items");
            rsItems.next();
            assertEquals("COD-1", rsItems.getString(1));
        }
    }

    @Test
    void restaurarCompletoReemplazaTodoElArchivo() throws Exception {
        File backupCompleto = new File(dbFile.getParentFile(), "backup_completo.db");
        BackupService.backupCompleto(backupCompleto);

        try (Connection conn = ConexionDB.getConexion(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM items");
        }
        ConexionDB.resetParaTests();

        BackupService.restaurarCompleto(backupCompleto);
        ConexionDB.resetParaTests();

        assertEquals(1, contarFilas(dbFile, "items"));
    }

    private int contarFilas(File archivoDb, String tabla) throws SQLException {
        try (Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + archivoDb.getAbsolutePath());
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
