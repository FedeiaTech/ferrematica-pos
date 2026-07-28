package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfiguracionDAO {

    private static final Logger LOGGER = Logger.getLogger(ConfiguracionDAO.class.getName());

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
                config.setRutaLogo(rs.getString("ruta_logo"));
                config.setMensajeTicket(rs.getString("mensaje_ticket"));
                config.setPermitirStockNegativo(rs.getInt("permitir_stock_negativo") == 1);
                config.setRecargoTarjeta(rs.getDouble("recargo_tarjeta"));
                config.setRutaBackup(rs.getString("ruta_backup"));
                config.setRutaGuardadoTickets(rs.getString("ruta_tickets"));
                String colorTema = rs.getString("color_tema");
                config.setColorTema(colorTema != null ? colorTema : "#f4f6f8");
                config.setPremiumDesbloqueado(rs.getInt("premium_desbloqueado") == 1);
                config.setAnchoTicketMm(rs.getInt("ancho_ticket_mm") == 0 ? 80 : rs.getInt("ancho_ticket_mm"));
                config.setTicketMostrarDireccion(rs.getInt("ticket_mostrar_direccion") != 0);
                config.setTicketMostrarCuit(rs.getInt("ticket_mostrar_cuit") != 0);
                config.setUsarEnteros(rs.getInt("usar_enteros") == 1);
                config.setMargenGananciaPct(rs.getDouble("margen_ganancia_pct"));
                String perfilNegocio = rs.getString("perfil_negocio");
                config.setPerfilNegocio(perfilNegocio != null ? perfilNegocio : "GENERICO");
            }
        }
        return config;
    }

    public void guardarConfiguracion(Configuracion config) throws SQLException {
        String sql = "UPDATE configuracion SET "
                   + "nombre_empresa=?, cuit=?, direccion=?, condicion_iva=?, punto_venta=?, "
                   + "certificado_ruta=?, "
                   + "ruta_logo=?, mensaje_ticket=?, permitir_stock_negativo=?, recargo_tarjeta=?, "
                   + "ruta_backup=?, ruta_tickets=?, "
                   + "ancho_ticket_mm=?, ticket_mostrar_direccion=?, ticket_mostrar_cuit=?, "
                   + "usar_enteros=?, margen_ganancia_pct=?, perfil_negocio=? "
                   + "WHERE id=1";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, config.getNombreEmpresa());
            pstmt.setString(2, config.getCuit());
            pstmt.setString(3, config.getDireccion());
            pstmt.setString(4, config.getCondicionIva());
            pstmt.setInt(5, config.getPuntoVenta());
            pstmt.setString(6, config.getCertificadoRuta());
            pstmt.setString(7, config.getRutaLogo());
            pstmt.setString(8, config.getMensajeTicket());
            pstmt.setInt(9, config.isPermitirStockNegativo() ? 1 : 0);
            pstmt.setDouble(10, config.getRecargoTarjeta());
            pstmt.setString(11, config.getRutaBackup());
            pstmt.setString(12, config.getRutaGuardadoTickets());
            pstmt.setInt(13, config.getAnchoTicketMm());
            pstmt.setInt(14, config.isTicketMostrarDireccion() ? 1 : 0);
            pstmt.setInt(15, config.isTicketMostrarCuit() ? 1 : 0);
            pstmt.setInt(16, config.isUsarEnteros() ? 1 : 0);
            pstmt.setDouble(17, config.getMargenGananciaPct());
            pstmt.setString(18, config.getPerfilNegocio());

            pstmt.executeUpdate();
        }
    }

    public void inicializarTabla() {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS configuracion ("
                         + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                         + "nombre_empresa TEXT, cuit TEXT, direccion TEXT, "
                         + "condicion_iva TEXT, punto_venta INTEGER DEFAULT 1, "
                         + "certificado_ruta TEXT)";

        String sqlInsert = "INSERT OR IGNORE INTO configuracion (id, nombre_empresa, cuit, direccion, condicion_iva) "
                         + "VALUES (1, 'Mi Negocio', '20-00000000-0', 'Sin Dirección', 'Consumidor Final')";

        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlCreate);
            stmt.execute(sqlInsert);
            actualizarTabla(conn);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la tabla configuracion", e);
        }
    }

    private void actualizarTabla(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ruta_logo TEXT"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN mensaje_ticket TEXT"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN permitir_stock_negativo INTEGER DEFAULT 1"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN recargo_tarjeta REAL DEFAULT 0.0"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ruta_backup TEXT"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ruta_tickets TEXT"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN color_tema TEXT DEFAULT '#ffffff'"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN premium_desbloqueado INTEGER DEFAULT 0"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ancho_ticket_mm INTEGER DEFAULT 80"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ticket_mostrar_direccion INTEGER DEFAULT 1"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN ticket_mostrar_cuit INTEGER DEFAULT 1"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN usar_enteros INTEGER DEFAULT 0"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN margen_ganancia_pct REAL DEFAULT 0.0"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE configuracion ADD COLUMN perfil_negocio TEXT DEFAULT 'GENERICO'"); } catch (SQLException e) {}
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al migrar columnas de configuracion", e);
        }
    }

    public void desbloquearPremium() throws SQLException {
        String sql = "UPDATE configuracion SET premium_desbloqueado = 1 WHERE id = 1";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }
}
