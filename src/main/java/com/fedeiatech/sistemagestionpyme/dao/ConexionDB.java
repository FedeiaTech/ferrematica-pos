package com.fedeiatech.sistemagestionpyme.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {

    private static final String URL = "jdbc:sqlite:gestion_pyme.db";
    private static Connection conexion = null;

    public static Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                conexion = DriverManager.getConnection(URL);
                System.out.println("Conexión a SQLite establecida.");
                inicializarTablas();
            } catch (ClassNotFoundException e) {
                throw new SQLException("No se encontró el driver de SQLite", e);
            }
        }
        return conexion;
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
    }

}