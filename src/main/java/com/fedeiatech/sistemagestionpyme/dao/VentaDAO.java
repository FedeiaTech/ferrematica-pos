package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.ComponenteCombo;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class VentaDAO {

    public void registrarVenta(Venta venta) throws SQLException {
        String sqlVenta = "INSERT INTO ventas (fecha, total) VALUES (?, ?)";
        String sqlDetalleItem  = "INSERT INTO detalles_venta (id_venta, id_item, id_combo, cantidad, precio_unitario, subtotal) VALUES (?, ?, NULL, ?, ?, ?)";
        String sqlDetalleCombo = "INSERT INTO detalles_venta (id_venta, id_item, id_combo, cantidad, precio_unitario, subtotal) VALUES (?, NULL, ?, ?, ?, ?)";
        String sqlStock = "UPDATE items SET stock = stock - ? WHERE id = ? AND es_servicio = 0";

        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);

            try (PreparedStatement pstVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                pstVenta.setString(1, venta.getFecha());
                pstVenta.setDouble(2, venta.getTotal());
                pstVenta.executeUpdate();
                try (ResultSet rs = pstVenta.getGeneratedKeys()) {
                    if (rs.next()) venta.setId(rs.getInt(1));
                }
            }

            try (PreparedStatement pstItem  = conn.prepareStatement(sqlDetalleItem);
                 PreparedStatement pstCombo = conn.prepareStatement(sqlDetalleCombo);
                 PreparedStatement pstStock = conn.prepareStatement(sqlStock)) {

                for (DetalleVenta detalle : venta.getDetalles()) {
                    if (detalle.esCombo()) {
                        pstCombo.setInt(1, venta.getId());
                        pstCombo.setInt(2, detalle.getCombo().getId());
                        pstCombo.setDouble(3, detalle.getCantidad());
                        pstCombo.setDouble(4, detalle.getPrecioUnitario());
                        pstCombo.setDouble(5, detalle.getSubtotal());
                        pstCombo.executeUpdate();

                        for (ComponenteCombo comp : detalle.getCombo().getComponentes()) {
                            pstStock.setDouble(1, comp.getCantidad() * detalle.getCantidad());
                            pstStock.setInt(2, comp.getIdItem());
                            pstStock.executeUpdate();
                        }
                    } else {
                        pstItem.setInt(1, venta.getId());
                        pstItem.setInt(2, detalle.getItem().getId());
                        pstItem.setDouble(3, detalle.getCantidad());
                        pstItem.setDouble(4, detalle.getPrecioUnitario());
                        pstItem.setDouble(5, detalle.getSubtotal());
                        pstItem.executeUpdate();

                        if (!detalle.getItem().isEsServicio()) {
                            pstStock.setDouble(1, detalle.getCantidad());
                            pstStock.setInt(2, detalle.getItem().getId());
                            pstStock.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public double sumarVentasDelDia() throws SQLException {
        double total = 0.0;
        String fechaHoy = java.time.LocalDate.now().toString();
        String sql = "SELECT SUM(total) FROM ventas WHERE fecha LIKE ?";

        try (Connection conn = ConexionDB.getConexion(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fechaHoy + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getDouble(1);
                }
            }
        }
        return total;
    }

    public java.util.List<Venta> listarVentasHistoricas() throws SQLException {
        java.util.List<Venta> lista = new java.util.ArrayList<>();
        String sql = "SELECT * FROM ventas ORDER BY fecha DESC";

        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Venta v = new Venta();
                v.setId(rs.getInt("id"));
                v.setFecha(rs.getString("fecha"));
                v.setTotal(rs.getDouble("total"));
                lista.add(v);
            }
        }
        return lista;
    }

    public int contarVentasDelDia() throws SQLException {
        String fechaHoy = java.time.LocalDate.now().toString();
        String sql = "SELECT COUNT(*) FROM ventas WHERE fecha LIKE ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fechaHoy + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public double obtenerGananciaEstimadaDelDia() throws SQLException {
        String fechaHoy = java.time.LocalDate.now().toString();
        String sql = "SELECT SUM((d.precio_unitario - i.precio_costo) * d.cantidad) " +
                     "FROM detalles_venta d " +
                     "JOIN items i ON d.id_item = i.id " +
                     "JOIN ventas v ON d.id_venta = v.id " +
                     "WHERE v.fecha LIKE ? AND i.es_servicio = 0 AND d.id_item IS NOT NULL";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fechaHoy + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    public int contarItemsStockCritico() throws SQLException {
        String sql = "SELECT COUNT(*) FROM items WHERE es_servicio = 0 AND stock <= 5";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public Map<String, Double> obtenerVentasUltimos7Dias() throws SQLException {
        Map<String, Double> resultado = new LinkedHashMap<>();
        String sql = "SELECT DATE(fecha) as dia, SUM(total) as total_dia " +
                     "FROM ventas " +
                     "WHERE DATE(fecha) >= DATE('now', '-6 days') " +
                     "GROUP BY DATE(fecha) " +
                     "ORDER BY dia ASC";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(rs.getString("dia"), rs.getDouble("total_dia"));
            }
        }
        return resultado;
    }

    public Map<String, Double> obtenerTop5ProductosMasVendidos() throws SQLException {
        Map<String, Double> resultado = new LinkedHashMap<>();
        String sql = "SELECT COALESCE(i.nombre, c.nombre) as nombre, SUM(d.cantidad) as total_vendido " +
                     "FROM detalles_venta d " +
                     "LEFT JOIN items i ON d.id_item = i.id " +
                     "LEFT JOIN combos c ON d.id_combo = c.id " +
                     "GROUP BY d.id_item, d.id_combo, nombre " +
                     "ORDER BY total_vendido DESC " +
                     "LIMIT 5";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(rs.getString("nombre"), rs.getDouble("total_vendido"));
            }
        }
        return resultado;
    }

    public java.util.List<String[]> obtenerResumenDiario(String desde, String hasta) throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        String sql = "SELECT DATE(fecha) as dia, COUNT(*) as cant, SUM(total) as total_dia " +
                     "FROM ventas WHERE DATE(fecha) BETWEEN ? AND ? " +
                     "GROUP BY DATE(fecha) ORDER BY dia ASC";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, desde);
            pst.setString(2, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new String[]{
                        rs.getString("dia"),
                        String.valueOf(rs.getInt("cant")),
                        String.valueOf(rs.getDouble("total_dia"))
                    });
                }
            }
        }
        return resultado;
    }

    public java.util.List<String[]> obtenerVentasPorProducto(String desde, String hasta) throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        String sql = "SELECT COALESCE(i.nombre, c.nombre) as nombre, COALESCE(i.unidad, 'u') as unidad, " +
                     "SUM(d.cantidad) as cant, SUM(d.subtotal) as total " +
                     "FROM detalles_venta d " +
                     "LEFT JOIN items i ON d.id_item = i.id " +
                     "LEFT JOIN combos c ON d.id_combo = c.id " +
                     "JOIN ventas v ON d.id_venta = v.id " +
                     "WHERE DATE(v.fecha) BETWEEN ? AND ? " +
                     "GROUP BY d.id_item, d.id_combo, nombre, unidad " +
                     "ORDER BY cant DESC";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, desde);
            pst.setString(2, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new String[]{
                        rs.getString("nombre"),
                        rs.getString("unidad"),
                        String.valueOf(rs.getDouble("cant")),
                        String.valueOf(rs.getDouble("total"))
                    });
                }
            }
        }
        return resultado;
    }

    public java.util.List<String[]> obtenerDetalleCompleto(String desde, String hasta) throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        String sql = "SELECT DATE(v.fecha) as dia, v.id, " +
                     "COALESCE(i.nombre, c.nombre || ' (COMBO)') as nombre, " +
                     "d.cantidad, COALESCE(i.unidad, 'u') as unidad, d.precio_unitario, d.subtotal " +
                     "FROM detalles_venta d " +
                     "LEFT JOIN items i ON d.id_item = i.id " +
                     "LEFT JOIN combos c ON d.id_combo = c.id " +
                     "JOIN ventas v ON d.id_venta = v.id " +
                     "WHERE DATE(v.fecha) BETWEEN ? AND ? " +
                     "ORDER BY v.fecha ASC, v.id ASC";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, desde);
            pst.setString(2, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new String[]{
                        rs.getString("dia"),
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nombre"),
                        String.valueOf(rs.getDouble("cantidad")),
                        rs.getString("unidad"),
                        String.valueOf(rs.getDouble("precio_unitario")),
                        String.valueOf(rs.getDouble("subtotal"))
                    });
                }
            }
        }
        return resultado;
    }

    public Venta obtenerVentaCompleta(int idVenta) throws SQLException {
        Venta venta = null;
        String sqlVenta = "SELECT * FROM ventas WHERE id = ?";
        String sqlDetalles = "SELECT d.cantidad, d.precio_unitario, d.id_item, d.id_combo, " +
                             "COALESCE(i.codigo, c.codigo) as codigo, " +
                             "COALESCE(i.nombre, c.nombre) as nombre " +
                             "FROM detalles_venta d " +
                             "LEFT JOIN items i ON d.id_item = i.id " +
                             "LEFT JOIN combos c ON d.id_combo = c.id " +
                             "WHERE d.id_venta = ?";

        try (Connection conn = ConexionDB.getConexion()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sqlVenta)) {
                pstmt.setInt(1, idVenta);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    venta = new Venta();
                    venta.setId(rs.getInt("id"));
                    venta.setFecha(rs.getString("fecha"));
                    venta.setTotal(rs.getDouble("total"));
                }
            }

            if (venta != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDetalles)) {
                    pstmt.setInt(1, idVenta);
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        com.fedeiatech.sistemagestionpyme.model.DetalleVenta detalle;
                        if (rs.getObject("id_combo") != null) {
                            com.fedeiatech.sistemagestionpyme.model.Combo combo = new com.fedeiatech.sistemagestionpyme.model.Combo();
                            combo.setCodigo(rs.getString("codigo"));
                            combo.setNombre(rs.getString("nombre"));
                            detalle = new com.fedeiatech.sistemagestionpyme.model.DetalleVenta(combo, rs.getDouble("cantidad"));
                        } else {
                            com.fedeiatech.sistemagestionpyme.model.ItemVenta item = new com.fedeiatech.sistemagestionpyme.model.ItemVenta();
                            item.setCodigo(rs.getString("codigo"));
                            item.setNombre(rs.getString("nombre"));
                            detalle = new com.fedeiatech.sistemagestionpyme.model.DetalleVenta(item, rs.getDouble("cantidad"));
                        }
                        detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
                        venta.agregarDetalle(detalle);
                    }
                }
            }
        }
        return venta;
    }

    public int borrarTodasLasVentas() throws SQLException {
        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM detalles_venta");
                int ventas = stmt.executeUpdate("DELETE FROM ventas");
                conn.commit();
                return ventas;
            }
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public java.util.List<String[]> obtenerVentasPorSemana() throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        String sql = "SELECT strftime('%Y-W%W', fecha) as semana, " +
                     "COUNT(*) as cant, SUM(total) as total_semana, AVG(total) as promedio " +
                     "FROM ventas GROUP BY semana ORDER BY semana DESC LIMIT 16";
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
        java.util.Collections.reverse(resultado);
        return resultado;
    }

    public java.util.List<String[]> obtenerVentasPorMes() throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        String sql = "SELECT strftime('%Y-%m', fecha) as mes, " +
                     "COUNT(*) as cant, SUM(total) as total_mes, " +
                     "SUM(d.subtotal) as total_productos " +
                     "FROM ventas v " +
                     "LEFT JOIN detalles_venta d ON d.id_venta = v.id " +
                     "GROUP BY mes ORDER BY mes DESC LIMIT 12";
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
        java.util.Collections.reverse(resultado);
        return resultado;
    }

    public java.util.List<String[]> obtenerMarketBasket() throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        String sql = "SELECT a.nombre AS prod_a, b.nombre AS prod_b, COUNT(*) AS frec " +
                     "FROM detalles_venta da " +
                     "JOIN detalles_venta db ON da.id_venta = db.id_venta AND da.id_item < db.id_item " +
                     "JOIN items a ON da.id_item = a.id " +
                     "JOIN items b ON db.id_item = b.id " +
                     "WHERE da.id_item IS NOT NULL AND db.id_item IS NOT NULL " +
                     "GROUP BY da.id_item, db.id_item " +
                     "HAVING frec >= 2 " +
                     "ORDER BY frec DESC LIMIT 20";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(new String[]{ rs.getString("prod_a"), rs.getString("prod_b"), String.valueOf(rs.getInt("frec")) });
            }
        }
        return resultado;
    }

    public Map<Integer, Double> obtenerTotalesPorHora() throws SQLException {
        Map<Integer, Double> resultado = new LinkedHashMap<>();
        String sql = "SELECT CAST(strftime('%H', fecha) AS INTEGER) as hora, SUM(total) as total_hora " +
                     "FROM ventas GROUP BY hora ORDER BY hora ASC";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(rs.getInt("hora"), rs.getDouble("total_hora"));
            }
        }
        return resultado;
    }

    public Map<String, Double> obtenerHeatmapDiaHora() throws SQLException {
        Map<String, Double> resultado = new LinkedHashMap<>();
        String sql = "SELECT CAST(strftime('%w', fecha) AS INTEGER) as dia, " +
                     "CAST(strftime('%H', fecha) AS INTEGER) as hora, " +
                     "COUNT(*) as cant " +
                     "FROM ventas GROUP BY dia, hora ORDER BY dia, hora";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.put(rs.getInt("dia") + "-" + rs.getInt("hora"), rs.getDouble("cant"));
            }
        }
        return resultado;
    }
}
