package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConfiguracionDAO {

    // LEER CONFIGURACIÓN (SELECT)
    public Configuracion obtenerConfiguracion() throws SQLException {
        String sql = "SELECT * FROM configuracion WHERE id = 1";
        Configuracion config = null;

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                config = new Configuracion();
                
                // Campos básicos (Tab 1)
                config.setNombreEmpresa(rs.getString("nombre_empresa"));
                config.setCuit(rs.getString("cuit"));
                config.setDireccion(rs.getString("direccion"));
                config.setCondicionIva(rs.getString("condicion_iva"));
                config.setPuntoVenta(rs.getInt("punto_venta"));
                config.setCertificadoRuta(rs.getString("certificado_ruta"));

                // --- NUEVOS CAMPOS (Tab 2 y 3) ---
                config.setRutaLogo(rs.getString("ruta_logo"));
                config.setMensajeTicket(rs.getString("mensaje_ticket"));
                
                // SQLite guarda booleans como 1 (true) o 0 (false)
                config.setPermitirStockNegativo(rs.getInt("permitir_stock_negativo") == 1);
                
                config.setRecargoTarjeta(rs.getDouble("recargo_tarjeta"));
                config.setRutaBackup(rs.getString("ruta_backup"));
                
                // NUEVO: Ruta de guardado de tickets
                config.setRutaGuardadoTickets(rs.getString("ruta_tickets"));
            }
        }
        return config;
    }

    // GUARDAR CONFIGURACIÓN (UPDATE)
    public void guardarConfiguracion(Configuracion config) throws SQLException {
        // Actualizamos TODOS los campos
        String sql = "UPDATE configuracion SET "
                   + "nombre_empresa=?, cuit=?, direccion=?, condicion_iva=?, punto_venta=?, "
                   + "ruta_logo=?, mensaje_ticket=?, permitir_stock_negativo=?, recargo_tarjeta=?, ruta_backup=?, "
                   + "ruta_tickets=? " // <--- NUEVO CAMPO (Index 11)
                   + "WHERE id=1";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Tab 1
            pstmt.setString(1, config.getNombreEmpresa());
            pstmt.setString(2, config.getCuit());
            pstmt.setString(3, config.getDireccion());
            pstmt.setString(4, config.getCondicionIva());
            pstmt.setInt(5, config.getPuntoVenta());
            
            // Tab 2
            pstmt.setString(6, config.getRutaLogo());
            pstmt.setString(7, config.getMensajeTicket());
            
            // Tab 3 (Convertir boolean a int)
            pstmt.setInt(8, config.isPermitirStockNegativo() ? 1 : 0);
            
            pstmt.setDouble(9, config.getRecargoTarjeta());
            pstmt.setString(10, config.getRutaBackup());
            
            // NUEVO: Guardar ruta de tickets
            pstmt.setString(11, config.getRutaGuardadoTickets());
            
            pstmt.executeUpdate();
        }
    }
    
    // MIGRACIÓN Y CREACIÓN DE TABLA
    public void inicializarTabla() {
        // Estructura básica original
        String sqlCreate = "CREATE TABLE IF NOT EXISTS configuracion ("
                         + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                         + "nombre_empresa TEXT, cuit TEXT, direccion TEXT, "
                         + "condicion_iva TEXT, punto_venta INTEGER DEFAULT 1, "
                         + "certificado_ruta TEXT)";
        
        // Fila por defecto
        String sqlInsert = "INSERT OR IGNORE INTO configuracion (id, nombre_empresa, cuit, direccion, condicion_iva) "
                         + "VALUES (1, 'Mi Negocio', '20-00000000-0', 'Sin Dirección', 'Consumidor Final')";
        
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sqlCreate);
            stmt.execute(sqlInsert);
            
            // IMPORTANTE: Llamamos a la actualización para crear las columnas nuevas
            actualizarTabla(conn);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método para agregar columnas nuevas si no existen (Migración)
    private void actualizarTabla(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Intentamos agregar cada columna nueva. Si ya existe, SQLite ignorará el error o lanzará uno controlable.
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ruta_logo TEXT"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN mensaje_ticket TEXT"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN permitir_stock_negativo INTEGER DEFAULT 1"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN recargo_tarjeta REAL DEFAULT 0.0"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ruta_backup TEXT"); } catch (SQLException e) {}
            // NUEVO:
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ruta_tickets TEXT"); } catch (SQLException e) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}