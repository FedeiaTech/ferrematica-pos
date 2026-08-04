package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    public void guardar(ItemVenta item) throws SQLException {
        String sql = "INSERT INTO items (codigo, nombre, descripcion, precio_costo, precio_venta, stock, es_servicio, unidad, categoria) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getCodigo());
            pstmt.setString(2, item.getNombre());
            pstmt.setString(3, item.getDescripcion());
            pstmt.setDouble(4, item.getPrecioCosto());
            pstmt.setDouble(5, item.getPrecioVenta());
            pstmt.setDouble(6, item.getStock());
            pstmt.setInt(7, item.isEsServicio() ? 1 : 0);
            pstmt.setString(8, item.getUnidad());
            pstmt.setString(9, item.getCategoria());

            pstmt.executeUpdate();
        }
    }

    public void actualizar(ItemVenta item) throws SQLException {
        String sql = "UPDATE items SET codigo=?, nombre=?, descripcion=?, precio_costo=?, precio_venta=?, stock=?, es_servicio=?, unidad=?, categoria=? WHERE id=?";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getCodigo());
            pstmt.setString(2, item.getNombre());
            pstmt.setString(3, item.getDescripcion());
            pstmt.setDouble(4, item.getPrecioCosto());
            pstmt.setDouble(5, item.getPrecioVenta());
            pstmt.setDouble(6, item.getStock());
            pstmt.setInt(7, item.isEsServicio() ? 1 : 0);
            pstmt.setString(8, item.getUnidad());
            pstmt.setString(9, item.getCategoria());
            pstmt.setInt(10, item.getId());

            pstmt.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        String sqlCodigo = "SELECT codigo FROM items WHERE id = ?";
        String sqlTombstone = "INSERT OR REPLACE INTO items_eliminados (codigo, eliminado_en) VALUES (?, ?)";
        String sqlDelete = "DELETE FROM items WHERE id = ?";

        try (Connection conn = ConexionDB.getConexion()) {
            String codigo = null;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCodigo)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        codigo = rs.getString("codigo");
                    }
                }
            }

            if (codigo != null && !codigo.trim().isEmpty()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlTombstone)) {
                    pstmt.setString(1, codigo);
                    pstmt.setString(2, Instant.now().toString());
                    pstmt.executeUpdate();
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlDelete)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
        }
    }

    public List<String> validarCodigosParaSync() throws SQLException {
        List<String> offenders = new ArrayList<>();
        String sql = "SELECT codigo, nombre FROM items "
                + "WHERE codigo IS NULL OR trim(codigo) = '' "
                + "   OR codigo IN (SELECT codigo FROM items GROUP BY codigo HAVING COUNT(*) > 1) "
                + "ORDER BY codigo";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String nombre = rs.getString("nombre");
                String codigoMostrado = (codigo == null || codigo.trim().isEmpty()) ? "(vacío)" : codigo;
                offenders.add(codigoMostrado + " | " + nombre);
            }
        }
        return offenders;
    }

    public List<String> listarCodigosEliminadosPendientes() throws SQLException {
        List<String> codigos = new ArrayList<>();
        String sql = "SELECT codigo FROM items_eliminados";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                codigos.add(rs.getString("codigo"));
            }
        }
        return codigos;
    }

    public void limpiarEliminadosSincronizados(List<String> codigos) throws SQLException {
        if (codigos == null || codigos.isEmpty()) {
            return;
        }

        String sql = "DELETE FROM items_eliminados WHERE codigo = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String codigo : codigos) {
                pstmt.setString(1, codigo);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public ItemVenta buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT * FROM items WHERE codigo = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codigo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ItemVenta item = new ItemVenta();
                    item.setId(rs.getInt("id"));
                    item.setCodigo(rs.getString("codigo"));
                    item.setNombre(rs.getString("nombre"));
                    item.setDescripcion(rs.getString("descripcion"));
                    item.setPrecioCosto(rs.getDouble("precio_costo"));
                    item.setPrecioVenta(rs.getDouble("precio_venta"));
                    item.setStock(rs.getDouble("stock"));
                    item.setEsServicio(rs.getInt("es_servicio") == 1);
                    String unidad = rs.getString("unidad");
                    item.setUnidad(unidad != null ? unidad : "u");
                    item.setCategoria(rs.getString("categoria"));
                    return item;
                }
            }
        }
        return null;
    }

    public List<ItemVenta> listarTodos() throws SQLException {
        List<ItemVenta> lista = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                ItemVenta item = new ItemVenta();
                item.setId(rs.getInt("id"));
                item.setCodigo(rs.getString("codigo"));
                item.setNombre(rs.getString("nombre"));
                item.setDescripcion(rs.getString("descripcion"));
                item.setPrecioCosto(rs.getDouble("precio_costo"));
                item.setPrecioVenta(rs.getDouble("precio_venta"));
                item.setStock(rs.getDouble("stock"));
                item.setEsServicio(rs.getInt("es_servicio") == 1);
                String unidad = rs.getString("unidad");
                item.setUnidad(unidad != null ? unidad : "u");
                item.setCategoria(rs.getString("categoria"));
                lista.add(item);
            }
        }
        return lista;
    }
}