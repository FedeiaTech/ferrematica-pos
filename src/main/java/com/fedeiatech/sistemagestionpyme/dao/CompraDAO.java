package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Compra;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompraDAO {

    private static final Logger LOGGER = Logger.getLogger(CompraDAO.class.getName());

    /**
     * Transacción atómica calcada de VentaDAO.registrarVenta, en sentido inverso
     * (suma stock en vez de restar). Diferencia deliberada: acá SÍ se valida el
     * resultado del UPDATE de stock — 0 filas afectadas significa que el id_item
     * no existe o es un servicio, y eso invalida toda la compra (a diferencia de
     * una venta de servicio, que legítimamente no toca stock). Solo ADMIN puede
     * registrar compras (chequeo server-side vía SessionService, no depende
     * únicamente de que la UI oculte el formulario).
     */
    public void registrarCompra(Compra compra) throws SQLException {
        if (!SessionService.getInstance().esAdmin()) {
            throw new SecurityException("Solo un administrador puede registrar compras.");
        }

        String sqlCompra = "INSERT INTO compras (id_item, cantidad, costo_unitario, costo_total, proveedor, fecha) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE items SET stock = stock + ?, precio_costo = ? WHERE id = ? AND es_servicio = 0";

        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);

            try (PreparedStatement pstCompra = conn.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS)) {
                pstCompra.setInt(1, compra.getIdItem());
                pstCompra.setDouble(2, compra.getCantidad());
                pstCompra.setDouble(3, compra.getCostoUnitario());
                pstCompra.setDouble(4, compra.getCostoTotal());
                pstCompra.setString(5, compra.getProveedor());
                pstCompra.setString(6, compra.getFecha());
                pstCompra.executeUpdate();
                try (ResultSet rs = pstCompra.getGeneratedKeys()) {
                    if (rs.next()) compra.setId(rs.getInt(1));
                }
            }

            try (PreparedStatement pstStock = conn.prepareStatement(sqlStock)) {
                pstStock.setDouble(1, compra.getCantidad());
                pstStock.setDouble(2, compra.getCostoUnitario());
                pstStock.setInt(3, compra.getIdItem());
                if (pstStock.executeUpdate() == 0) {
                    throw new SQLException("El producto no existe o es un servicio: id=" + compra.getIdItem());
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error al hacer rollback de la compra", ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public double sumarComprasEntre(String desde, String hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(costo_total), 0) FROM compras WHERE DATE(fecha) BETWEEN ? AND ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, desde);
            pst.setString(2, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    public List<String[]> obtenerComprasPorSemana() throws SQLException {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT strftime('%Y-W%W', fecha) as semana, "
                + "COUNT(*) as cant, SUM(costo_total) as total_semana, AVG(costo_total) as promedio "
                + "FROM compras GROUP BY semana ORDER BY semana DESC LIMIT 16";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(new String[]{
                    rs.getString("semana"),
                    String.valueOf(rs.getInt("cant")),
                    String.valueOf(rs.getDouble("total_semana")),
                    String.valueOf(rs.getDouble("promedio"))
                });
            }
        }
        Collections.reverse(resultado);
        return resultado;
    }

    public List<String[]> obtenerComprasPorMes() throws SQLException {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT strftime('%Y-%m', fecha) as mes, "
                + "COUNT(*) as cant, SUM(costo_total) as total_mes "
                + "FROM compras GROUP BY mes ORDER BY mes DESC LIMIT 12";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(new String[]{
                    rs.getString("mes"),
                    String.valueOf(rs.getInt("cant")),
                    String.valueOf(rs.getDouble("total_mes"))
                });
            }
        }
        Collections.reverse(resultado);
        return resultado;
    }

    /** LEFT JOIN obligatorio: una compra debe sobrevivir al borrado del item referenciado (ver ADR-5 del diseño). */
    public List<Compra> listarHistorico(int limite) throws SQLException {
        List<Compra> resultado = new ArrayList<>();
        String sql = "SELECT c.*, COALESCE(i.nombre, '(producto eliminado)') AS nombre_item "
                + "FROM compras c LEFT JOIN items i ON c.id_item = i.id "
                + "ORDER BY c.fecha DESC, c.id DESC LIMIT ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, limite);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Compra c = new Compra();
                    c.setId(rs.getInt("id"));
                    c.setIdItem(rs.getInt("id_item"));
                    c.setNombreItem(rs.getString("nombre_item"));
                    c.setCantidad(rs.getDouble("cantidad"));
                    c.setCostoUnitario(rs.getDouble("costo_unitario"));
                    c.setCostoTotal(rs.getDouble("costo_total"));
                    c.setProveedor(rs.getString("proveedor"));
                    c.setFecha(rs.getString("fecha"));
                    resultado.add(c);
                }
            }
        }
        return resultado;
    }
}
