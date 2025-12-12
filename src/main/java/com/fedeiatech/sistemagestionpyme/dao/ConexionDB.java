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

    // Este método es la magia que hace tu producto "portable"
    private static void inicializarTablas() throws SQLException {
        Statement stmt = conexion.createStatement();
        
        // SQL para crear la tabla de items si no existe
        String sqlItems = "CREATE TABLE IF NOT EXISTS items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "codigo TEXT UNIQUE NOT NULL," +
                "nombre TEXT NOT NULL," +
                "descripcion TEXT," +
                "precio_costo REAL," +
                "precio_venta REAL," +
                "stock REAL," +
                "es_servicio INTEGER" + // SQLite no tiene boolean, usamos 0 o 1
                ");";
        
        stmt.execute(sqlItems);
        // Aquí agregarías más tablas en el futuro (clientes, ventas...)
    }
}