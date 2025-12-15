package com.fedeiatech.sistemagestionpyme.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {

    // Nombre del archivo de la base de datos. Se creará en la carpeta del proyecto.
    private static final String URL = "jdbc:sqlite:gestion_pyme.db";
    private static Connection conexion = null;

    // Método para obtener la conexión (Patrón Singleton simple)
    public static Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            try {
                // Esto asegura que el driver de SQLite se cargue
                Class.forName("org.sqlite.JDBC");
                conexion = DriverManager.getConnection(URL);
                System.out.println("Conexión a SQLite establecida.");

                // Al conectar, verificamos que las tablas existan
                inicializarTablas();

            } catch (ClassNotFoundException e) {
                throw new SQLException("No se encontró el driver de SQLite", e);
            }
        }
        return conexion;
    }

    private static void inicializarTablas() throws SQLException {
        Statement stmt = conexion.createStatement();

        // 1. Tabla ITEMS (Ya la tenías)
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

        // 2. NUEVA: Tabla VENTAS (La cabecera del ticket)
        String sqlVentas = "CREATE TABLE IF NOT EXISTS ventas ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "fecha TEXT NOT NULL,"
                + // Guardaremos fecha como texto ISO8601
                "total REAL NOT NULL"
                + ");";
        stmt.execute(sqlVentas);

        // 3. NUEVA: Tabla DETALLES (Renglones del ticket)
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
    }

}
