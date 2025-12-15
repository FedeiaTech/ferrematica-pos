package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class VentaDAO {

    public void registrarVenta(Venta venta) throws SQLException {
        String sqlVenta = "INSERT INTO ventas (fecha, total) VALUES (?, ?)";
        String sqlDetalle = "INSERT INTO detalles_venta (id_venta, id_item, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE items SET stock = stock - ? WHERE id = ? AND es_servicio = 0";

        Connection conn = null;

        try {
            conn = ConexionDB.getConexion();
            // 1. INICIO DE TRANSACCIÓN (Apagamos el guardado automático)
            conn.setAutoCommit(false);

            // 2. Guardar Cabecera (Venta)
            // RETURN_GENERATED_KEYS es para recuperar el ID (ej: Venta N° 50) que SQLite acaba de crear
            try (PreparedStatement pstVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                pstVenta.setString(1, venta.getFecha());
                pstVenta.setDouble(2, venta.getTotal());
                pstVenta.executeUpdate();

                // Recuperamos el ID generado
                try (ResultSet rs = pstVenta.getGeneratedKeys()) {
                    if (rs.next()) {
                        venta.setId(rs.getInt(1));
                    }
                }
            }

            // 3. Guardar Detalles y Descontar Stock
            try (PreparedStatement pstDetalle = conn.prepareStatement(sqlDetalle);
                 PreparedStatement pstStock = conn.prepareStatement(sqlStock)) {

                for (DetalleVenta detalle : venta.getDetalles()) {
                    // A. Insertar detalle
                    pstDetalle.setInt(1, venta.getId());
                    pstDetalle.setInt(2, detalle.getItem().getId());
                    pstDetalle.setDouble(3, detalle.getCantidad());
                    pstDetalle.setDouble(4, detalle.getPrecioUnitario());
                    pstDetalle.setDouble(5, detalle.getSubtotal());
                    pstDetalle.executeUpdate();

                    // B. Descontar stock (Solo si NO es servicio)
                    // Si es servicio, el stock es -1 o infinito, no lo tocamos
                    if (!detalle.getItem().isEsServicio()) {
                        pstStock.setDouble(1, detalle.getCantidad());
                        pstStock.setInt(2, detalle.getItem().getId());
                        pstStock.executeUpdate();
                    }
                }
            }

            // 4. CONFIRMAR TODO (Commit)
            conn.commit();
            System.out.println("Venta registrada con éxito. ID: " + venta.getId());

        } catch (SQLException e) {
            // 5. SI ALGO FALLA, DESHACER TODO (Rollback)
            if (conn != null) {
                try {
                    System.err.println("Error en transacción. Deshaciendo cambios...");
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e; // Relanzamos el error para que la Pantalla avise al usuario
        } finally {
            // Restaurar el modo normal
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }
}