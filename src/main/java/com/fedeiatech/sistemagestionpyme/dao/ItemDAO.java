package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // GUARDAR (Create)
    public void guardar(ItemVenta item) throws SQLException {
        String sql = "INSERT INTO items (codigo, nombre, descripcion, precio_costo, precio_venta, stock, es_servicio) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, item.getCodigo());
            pstmt.setString(2, item.getNombre());
            pstmt.setString(3, item.getDescripcion());
            pstmt.setDouble(4, item.getPrecioCosto());
            pstmt.setDouble(5, item.getPrecioVenta());
            pstmt.setDouble(6, item.getStock());
            pstmt.setInt(7, item.isEsServicio() ? 1 : 0); // Convertimos boolean a int
            
            pstmt.executeUpdate();
        }
    }
    
    // ELIMINAR (Delete)
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // LISTAR TODOS (Read)
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
                
                lista.add(item);
            }
        }
        return lista;
    }
}