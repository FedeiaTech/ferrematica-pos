package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.Combo;
import com.fedeiatech.sistemagestionpyme.model.ComponenteCombo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComboDAO {

    public void guardar(Combo combo) throws SQLException {
        String sqlCombo = "INSERT INTO combos (codigo, nombre, descripcion, precio_venta) VALUES (?, ?, ?, ?)";
        String sqlComp = "INSERT INTO combo_componentes (id_combo, id_item, cantidad) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);

            try (PreparedStatement pst = conn.prepareStatement(sqlCombo, Statement.RETURN_GENERATED_KEYS)) {
                pst.setString(1, combo.getCodigo());
                pst.setString(2, combo.getNombre());
                pst.setString(3, combo.getDescripcion());
                pst.setDouble(4, combo.getPrecioVenta());
                pst.executeUpdate();
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) combo.setId(rs.getInt(1));
                }
            }

            try (PreparedStatement pst = conn.prepareStatement(sqlComp)) {
                for (ComponenteCombo c : combo.getComponentes()) {
                    pst.setInt(1, combo.getId());
                    pst.setInt(2, c.getIdItem());
                    pst.setDouble(3, c.getCantidad());
                    pst.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public void actualizar(Combo combo) throws SQLException {
        String sqlCombo = "UPDATE combos SET codigo=?, nombre=?, descripcion=?, precio_venta=? WHERE id=?";
        String sqlBorrarComp = "DELETE FROM combo_componentes WHERE id_combo=?";
        String sqlComp = "INSERT INTO combo_componentes (id_combo, id_item, cantidad) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);

            try (PreparedStatement pst = conn.prepareStatement(sqlCombo)) {
                pst.setString(1, combo.getCodigo());
                pst.setString(2, combo.getNombre());
                pst.setString(3, combo.getDescripcion());
                pst.setDouble(4, combo.getPrecioVenta());
                pst.setInt(5, combo.getId());
                pst.executeUpdate();
            }
            try (PreparedStatement pst = conn.prepareStatement(sqlBorrarComp)) {
                pst.setInt(1, combo.getId());
                pst.executeUpdate();
            }
            try (PreparedStatement pst = conn.prepareStatement(sqlComp)) {
                for (ComponenteCombo c : combo.getComponentes()) {
                    pst.setInt(1, combo.getId());
                    pst.setInt(2, c.getIdItem());
                    pst.setDouble(3, c.getCantidad());
                    pst.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public void eliminar(int idCombo) throws SQLException {
        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);
            try (PreparedStatement pst = conn.prepareStatement("DELETE FROM combo_componentes WHERE id_combo=?")) {
                pst.setInt(1, idCombo);
                pst.executeUpdate();
            }
            try (PreparedStatement pst = conn.prepareStatement("DELETE FROM combos WHERE id=?")) {
                pst.setInt(1, idCombo);
                pst.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public List<Combo> listarTodos() throws SQLException {
        List<Combo> lista = new ArrayList<>();
        String sql = "SELECT * FROM combos ORDER BY nombre";
        String sqlComp = "SELECT cc.id_item, cc.cantidad, i.codigo, i.nombre "
                       + "FROM combo_componentes cc JOIN items i ON cc.id_item = i.id "
                       + "WHERE cc.id_combo = ?";

        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Combo c = mapear(rs);
                try (PreparedStatement pst = conn.prepareStatement(sqlComp)) {
                    pst.setInt(1, c.getId());
                    try (ResultSet rc = pst.executeQuery()) {
                        while (rc.next()) {
                            c.agregarComponente(new ComponenteCombo(
                                rc.getInt("id_item"),
                                rc.getString("codigo"),
                                rc.getString("nombre"),
                                rc.getDouble("cantidad")
                            ));
                        }
                    }
                }
                c.setStockCalculado(calcularStock(c.getId(), conn));
                lista.add(c);
            }
        }
        return lista;
    }

    public Combo buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT * FROM combos WHERE codigo = ?";
        String sqlComp = "SELECT cc.id_item, cc.cantidad, i.codigo, i.nombre "
                       + "FROM combo_componentes cc JOIN items i ON cc.id_item = i.id "
                       + "WHERE cc.id_combo = ?";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, codigo);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return null;
                Combo c = mapear(rs);
                try (PreparedStatement pstComp = conn.prepareStatement(sqlComp)) {
                    pstComp.setInt(1, c.getId());
                    try (ResultSet rc = pstComp.executeQuery()) {
                        while (rc.next()) {
                            c.agregarComponente(new ComponenteCombo(
                                rc.getInt("id_item"),
                                rc.getString("codigo"),
                                rc.getString("nombre"),
                                rc.getDouble("cantidad")
                            ));
                        }
                    }
                }
                c.setStockCalculado(calcularStock(c.getId(), conn));
                return c;
            }
        }
    }

    public int calcularStock(int idCombo) throws SQLException {
        try (Connection conn = ConexionDB.getConexion()) {
            return calcularStock(idCombo, conn);
        }
    }

    private int calcularStock(int idCombo, Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MIN(CAST(i.stock / cc.cantidad AS INTEGER)), 0) "
                   + "FROM combo_componentes cc "
                   + "JOIN items i ON cc.id_item = i.id "
                   + "WHERE cc.id_combo = ? AND i.es_servicio = 0 AND cc.cantidad > 0";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idCombo);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean esComponenteDeAlgunCombo(int idItem) throws SQLException {
        String sql = "SELECT COUNT(*) FROM combo_componentes WHERE id_item = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idItem);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Combo mapear(ResultSet rs) throws SQLException {
        Combo c = new Combo();
        c.setId(rs.getInt("id"));
        c.setCodigo(rs.getString("codigo"));
        c.setNombre(rs.getString("nombre"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setPrecioVenta(rs.getDouble("precio_venta"));
        return c;
    }
}
