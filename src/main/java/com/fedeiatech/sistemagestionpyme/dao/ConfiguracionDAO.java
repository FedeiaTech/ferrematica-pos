package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfiguracionDAO {

    // Obtener los datos del negocio
    public Configuracion obtenerConfiguracion() throws SQLException {
        String sql = "SELECT * FROM configuracion WHERE id = 1";
        Configuracion config = null;

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                config = new Configuracion();
                config.setNombreEmpresa(rs.getString("nombre_empresa"));
                config.setCuit(rs.getString("cuit"));
                config.setDireccion(rs.getString("direccion"));
                config.setCondicionIva(rs.getString("condicion_iva"));
                config.setPuntoVenta(rs.getInt("punto_venta"));
                config.setCertificadoRuta(rs.getString("certificado_ruta"));
            }
        }
        return config;
    }

    // Actualizar los datos
    public void guardarConfiguracion(Configuracion config) throws SQLException {
        // Usamos UPDATE porque la fila 1 ya debería existir (gracias al INSERT OR IGNORE inicial)
        String sql = "UPDATE configuracion SET nombre_empresa=?, cuit=?, direccion=?, condicion_iva=?, punto_venta=? WHERE id=1";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, config.getNombreEmpresa());
            pstmt.setString(2, config.getCuit());
            pstmt.setString(3, config.getDireccion());
            pstmt.setString(4, config.getCondicionIva());
            pstmt.setInt(5, config.getPuntoVenta());
            
            pstmt.executeUpdate();
        }
    }
    
    // Método auxiliar para crear la tabla si no existe (Útil para el primer inicio)
    public void inicializarTabla() {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS configuracion (id INTEGER PRIMARY KEY CHECK (id = 1), nombre_empresa TEXT, cuit TEXT, direccion TEXT, condicion_iva TEXT, punto_venta INTEGER DEFAULT 1, certificado_ruta TEXT)";
        String sqlInsert = "INSERT OR IGNORE INTO configuracion (id, nombre_empresa, cuit, direccion, condicion_iva) VALUES (1, 'Mi Negocio', '20-00000000-0', 'Sin Dirección', 'Consumidor Final')";
        
        try (Connection conn = ConexionDB.getConexion();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCreate);
            stmt.execute(sqlInsert);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}