package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ConexionDB;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Genera y restaura copias de seguridad de {@code gestion_pyme.db} en dos niveles:
 * {@code Completo} (copia cruda del archivo, incluye {@code configuracion} y {@code usuarios})
 * y {@code Solo datos} (únicamente las tablas de negocio — inventario, ventas, combos, compras,
 * gastos — sin tocar la configuración ni las cuentas de usuario de la instalación destino).
 *
 * <p>Sin estado — no hay razón para un singleton (a diferencia de {@link SupabaseSyncService},
 * que mantiene un scheduler vivo).
 */
public class BackupService {

    // Tablas de negocio (inventario/ventas/envíos) — ver ConexionDB.inicializarTablas().
    // "configuracion" y "usuarios" quedan afuera a propósito: son datos de instalación,
    // no datos del negocio, y un backup "solo datos" no debe pisarlos en el destino.
    private static final List<String> TABLAS_DATOS = List.of(
        "items", "items_eliminados", "ventas", "detalles_venta",
        "combos", "combo_componentes", "compras", "gastos");

    private BackupService() {}

    // Misma resolución que ConexionDB.urlActual() — nunca hardcodear "gestion_pyme.db" acá,
    // o un test (o una instalación con db.path custom) termina leyendo/pisando el archivo
    // equivocado en vez del que realmente tiene la conexión activa.
    private static File archivoDbActual() {
        return new File(System.getProperty("db.path", "gestion_pyme.db"));
    }

    public static void backupCompleto(File destino) throws IOException {
        Files.copy(archivoDbActual().toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void backupSoloDatos(File destino) throws SQLException {
        if (destino.exists() && !destino.delete()) {
            throw new SQLException("No se pudo sobrescribir el archivo de destino: " + destino.getAbsolutePath());
        }
        String rutaOrigen = archivoDbActual().getAbsoluteFile().getPath();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + destino.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("ATTACH DATABASE '" + rutaOrigen.replace("'", "''") + "' AS origen");
            try {
                for (String tabla : TABLAS_DATOS) {
                    stmt.execute("CREATE TABLE " + tabla + " AS SELECT * FROM origen." + tabla);
                }
            } finally {
                stmt.execute("DETACH DATABASE origen");
            }
        }
    }

    /**
     * Distingue el nivel de un backup mirando su propio esquema en vez de confiar en el
     * nombre del archivo — un usuario puede renombrarlo libremente. Un backup "solo datos"
     * (creado por {@link #backupSoloDatos}) nunca tiene {@code configuracion} ni
     * {@code usuarios}; un backup "completo" (copia cruda del .db en uso) siempre las tiene.
     */
    public static boolean esBackupCompleto(File backup) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + backup.getAbsolutePath());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('configuracion','usuarios')")) {
            rs.next();
            return rs.getInt(1) == 2;
        }
    }

    public static void restaurarCompleto(File origen) throws IOException {
        File destino = archivoDbActual().getAbsoluteFile();
        Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Fusiona sobre la conexión viva en vez de reemplazar el archivo: así {@code configuracion}
     * y {@code usuarios} de ESTA instalación no se tocan. No requiere reiniciar la app — los DAO
     * (ItemDAO, VentaDAO, etc.) siempre consultan la conexión activa, nunca cachean filas.
     */
    public static void restaurarSoloDatos(File origen) throws SQLException {
        Connection conn = ConexionDB.getConexion();
        boolean autoCommitPrevio = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ATTACH DATABASE '" + origen.getAbsolutePath().replace("'", "''") + "' AS origen");
            try {
                for (String tabla : TABLAS_DATOS) {
                    stmt.execute("DELETE FROM " + tabla);
                    stmt.execute("INSERT INTO " + tabla + " SELECT * FROM origen." + tabla);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                stmt.execute("DETACH DATABASE origen");
            }
        } finally {
            conn.setAutoCommit(autoCommitPrevio);
        }
    }
}
