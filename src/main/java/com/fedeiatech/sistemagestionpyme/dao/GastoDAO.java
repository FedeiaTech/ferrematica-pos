package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Gasto;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GastoDAO {

    /**
     * Solo ADMIN puede registrar gastos (chequeo server-side vía SessionService,
     * no depende únicamente de que la UI oculte el formulario). Mismo criterio
     * que VentaDAO.anularVenta.
     */
    public void registrar(Gasto gasto) throws SQLException {
        if (!SessionService.getInstance().esAdmin()) {
            throw new SecurityException("Solo un administrador puede registrar gastos.");
        }

        String sql = "INSERT INTO gastos (concepto, monto, categoria, fecha, usuario) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, gasto.getConcepto());
            pst.setDouble(2, gasto.getMonto());
            pst.setString(3, gasto.getCategoria());
            pst.setString(4, gasto.getFecha());
            pst.setString(5, gasto.getUsuario());
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) gasto.setId(rs.getInt(1));
            }
        }
    }

    public List<Gasto> listarEntre(String desde, String hasta) throws SQLException {
        List<Gasto> resultado = new ArrayList<>();
        String sql = "SELECT * FROM gastos WHERE DATE(fecha) BETWEEN ? AND ? ORDER BY fecha DESC, id DESC";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, desde);
            pst.setString(2, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        }
        return resultado;
    }

    public List<Gasto> listarTodos() throws SQLException {
        List<Gasto> resultado = new ArrayList<>();
        String sql = "SELECT * FROM gastos ORDER BY fecha DESC, id DESC";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    /**
     * Solo ADMIN puede eliminar gastos (chequeo server-side vía SessionService,
     * no depende únicamente de que la UI oculte la columna de acción).
     */
    public void eliminar(int id) throws SQLException {
        if (!SessionService.getInstance().esAdmin()) {
            throw new SecurityException("Solo un administrador puede eliminar gastos.");
        }

        String sql = "DELETE FROM gastos WHERE id = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    public double sumarGastosEntre(String desde, String hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM gastos WHERE DATE(fecha) BETWEEN ? AND ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, desde);
            pst.setString(2, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    private Gasto mapear(ResultSet rs) throws SQLException {
        Gasto g = new Gasto();
        g.setId(rs.getInt("id"));
        g.setConcepto(rs.getString("concepto"));
        g.setMonto(rs.getDouble("monto"));
        g.setCategoria(rs.getString("categoria"));
        g.setFecha(rs.getString("fecha"));
        g.setUsuario(rs.getString("usuario"));
        return g;
    }
}
