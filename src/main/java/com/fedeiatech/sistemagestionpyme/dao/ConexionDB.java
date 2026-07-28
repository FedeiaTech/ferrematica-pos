package com.fedeiatech.sistemagestionpyme.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {

    private static Connection conexion = null;

    private static String urlActual() {
        return "jdbc:sqlite:" + System.getProperty("db.path", "gestion_pyme.db");
    }

    public static synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                conexion = DriverManager.getConnection(urlActual());
                System.out.println("Conexión a SQLite establecida.");
                inicializarTablas();
            } catch (ClassNotFoundException e) {
                throw new SQLException("No se encontró el driver de SQLite", e);
            }
        }
        return conexion;
    }

    static synchronized void resetParaTests() throws SQLException {
        if (conexion != null && !conexion.isClosed()) conexion.close();
        conexion = null;
    }

    private static void inicializarTablas() throws SQLException {
        Statement stmt = conexion.createStatement();

        String sqlItems = "CREATE TABLE IF NOT EXISTS items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "codigo TEXT UNIQUE NOT NULL,"
                + "nombre TEXT NOT NULL,"
                + "descripcion TEXT,"
                + "precio_costo REAL,"
                + "precio_venta REAL,"
                + "stock REAL,"
                + "es_servicio INTEGER"
                + ");";
        stmt.execute(sqlItems);

        try {
            stmt.execute("ALTER TABLE items ADD COLUMN unidad TEXT DEFAULT 'u'");
        } catch (SQLException ignored) {
        }

        String sqlVentas = "CREATE TABLE IF NOT EXISTS ventas ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "fecha TEXT NOT NULL,"
                + "total REAL NOT NULL"
                + ");";
        stmt.execute(sqlVentas);

        String sqlDetalles = "CREATE TABLE IF NOT EXISTS detalles_venta ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "id_venta INTEGER NOT NULL,"
                + "id_item INTEGER NOT NULL,"
                + "cantidad REAL NOT NULL,"
                + "precio_unitario REAL NOT NULL,"
                + "subtotal REAL NOT NULL,"
                + "FOREIGN KEY(id_venta) REFERENCES ventas(id)"
                + ");";
        stmt.execute(sqlDetalles);

        try { migrarDetallesVenta(stmt); } catch (SQLException e) {
            System.err.println("Migración detalles_venta omitida: " + e.getMessage());
        }

        stmt.execute("CREATE TABLE IF NOT EXISTS combos ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "codigo TEXT UNIQUE NOT NULL,"
                + "nombre TEXT NOT NULL,"
                + "descripcion TEXT,"
                + "precio_venta REAL NOT NULL"
                + ");");

        stmt.execute("CREATE TABLE IF NOT EXISTS combo_componentes ("
                + "id_combo INTEGER NOT NULL,"
                + "id_item INTEGER NOT NULL,"
                + "cantidad REAL NOT NULL,"
                + "PRIMARY KEY (id_combo, id_item),"
                + "FOREIGN KEY(id_combo) REFERENCES combos(id),"
                + "FOREIGN KEY(id_item) REFERENCES items(id)"
                + ");");
    }

    private static void migrarDetallesVenta(Statement stmt) throws SQLException {
        boolean tieneIdCombo = false;
        try (java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info(detalles_venta)")) {
            while (rs.next()) {
                if ("id_combo".equals(rs.getString("name"))) {
                    tieneIdCombo = true;
                    break;
                }
            }
        }
        if (tieneIdCombo) return;

        conexion.setAutoCommit(false);
        try {
            stmt.execute("DROP TABLE IF EXISTS detalles_venta_new");
            stmt.execute("CREATE TABLE detalles_venta_new ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "id_venta INTEGER NOT NULL,"
                    + "id_item INTEGER,"
                    + "id_combo INTEGER,"
                    + "cantidad REAL NOT NULL,"
                    + "precio_unitario REAL NOT NULL,"
                    + "subtotal REAL NOT NULL,"
                    + "FOREIGN KEY(id_venta) REFERENCES ventas(id)"
                    + ")");
            stmt.execute("INSERT INTO detalles_venta_new (id, id_venta, id_item, cantidad, precio_unitario, subtotal) "
                    + "SELECT id, id_venta, id_item, cantidad, precio_unitario, subtotal FROM detalles_venta");
            stmt.execute("DROP TABLE detalles_venta");
            stmt.execute("ALTER TABLE detalles_venta_new RENAME TO detalles_venta");
            conexion.commit();
        } catch (SQLException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setAutoCommit(true);
        }
    }

}