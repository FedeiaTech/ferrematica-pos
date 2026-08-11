package com.fedeiatech.sistemagestionpyme.dao;

import com.fedeiatech.sistemagestionpyme.model.ComponenteCombo;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VentaDAO {

    private static final Logger LOGGER = Logger.getLogger(VentaDAO.class.getName());

    /** Excluye ventas anuladas de queries que ya filtran por columnas de "ventas" sin alias. */
    private static final String FILTRO_ACTIVAS = " AND estado <> '" + Venta.ESTADO_ANULADA + "'";
    /** Variante para queries con JOIN a "ventas v". */
    private static final String FILTRO_ACTIVAS_V = " AND v.estado <> '" + Venta.ESTADO_ANULADA + "'";
    /** Para queries sobre "ventas" sin WHERE previo. */
    private static final String WHERE_ACTIVAS = "WHERE estado <> '" + Venta.ESTADO_ANULADA + "' ";

    public void registrarVenta(Venta venta) throws SQLException {
        String sqlVenta = "INSERT INTO ventas (fecha, total, estado) VALUES (?, ?, ?)";
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
                pstVenta.setString(3, venta.getEstado() != null ? venta.getEstado() : Venta.ESTADO_COMPLETADA);
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
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Error al hacer rollback de la venta", ex); } }
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public double sumarVentasDelDia() throws SQLException {
        double total = 0.0;
        String fechaHoy = java.time.LocalDate.now().toString();
        String sql = "SELECT SUM(total) FROM ventas WHERE fecha LIKE ?" + FILTRO_ACTIVAS;

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

    /** Usado por Balances. No reusar obtenerVentasPorMes(): ese método tiene un JOIN que infla el total (ver comentario ahí). */
    public double sumarVentasEntre(String desde, String hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM ventas WHERE DATE(fecha) BETWEEN ? AND ?" + FILTRO_ACTIVAS;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, desde);
            pst.setString(2, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
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
                v.setEstado(rs.getString("estado"));
                v.setMotivoAnulacion(rs.getString("motivo_anulacion"));
                lista.add(v);
            }
        }
        return lista;
    }

    /** Usado por el desglose de Balance. Excluye ventas anuladas. */
    public java.util.List<Venta> listarVentasEntre(String desde, String hasta) throws SQLException {
        java.util.List<Venta> lista = new java.util.ArrayList<>();
        String sql = "SELECT * FROM ventas WHERE DATE(fecha) BETWEEN ? AND ?" + FILTRO_ACTIVAS + " ORDER BY fecha ASC";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, desde);
            pst.setString(2, hasta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Venta v = new Venta();
                    v.setId(rs.getInt("id"));
                    v.setFecha(rs.getString("fecha"));
                    v.setTotal(rs.getDouble("total"));
                    v.setEstado(rs.getString("estado"));
                    v.setMotivoAnulacion(rs.getString("motivo_anulacion"));
                    lista.add(v);
                }
            }
        }
        return lista;
    }

    /**
     * Anula una venta en una transacción única: marca estado='anulada' + motivo
     * (rechaza doble anulación vía guard AND estado='completada') y repone stock
     * de items físicos y de componentes de combo. Tolerante a items/combos
     * borrados del catálogo (0 filas afectadas no aborta la transacción) — mismo
     * criterio asimétrico que registrarVenta. Solo ADMIN puede anular
     * (chequeo server-side vía SessionService, no depende únicamente de que
     * la UI oculte el botón).
     */
    public boolean anularVenta(int idVenta, String motivo) throws SQLException {
        if (!SessionService.getInstance().esAdmin()) {
            return false;
        }

        // sincronizada_en se limpia acá (design decision D5): un cambio de estado a anulada
        // debe volver a empujarse a Supabase para que el mirror refleje la anulación.
        String sqlUpdateVenta = "UPDATE ventas SET estado = ?, motivo_anulacion = ?, sincronizada_en = NULL WHERE id = ? AND estado = ?";
        String sqlDetalles = "SELECT id_item, id_combo, cantidad FROM detalles_venta WHERE id_venta = ?";
        String sqlRestockItem = "UPDATE items SET stock = stock + ? WHERE id = ? AND es_servicio = 0";
        String sqlComponentesCombo = "SELECT id_item, cantidad FROM combo_componentes WHERE id_combo = ?";

        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);

            try (PreparedStatement pstUpdate = conn.prepareStatement(sqlUpdateVenta)) {
                pstUpdate.setString(1, Venta.ESTADO_ANULADA);
                pstUpdate.setString(2, motivo);
                pstUpdate.setInt(3, idVenta);
                pstUpdate.setString(4, Venta.ESTADO_COMPLETADA);
                int filas = pstUpdate.executeUpdate();
                if (filas == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement pstDetalles = conn.prepareStatement(sqlDetalles);
                 PreparedStatement pstRestock = conn.prepareStatement(sqlRestockItem);
                 PreparedStatement pstComponentes = conn.prepareStatement(sqlComponentesCombo)) {

                pstDetalles.setInt(1, idVenta);
                try (ResultSet rs = pstDetalles.executeQuery()) {
                    while (rs.next()) {
                        Object idItemObj = rs.getObject("id_item");
                        Object idComboObj = rs.getObject("id_combo");
                        double cantidad = rs.getDouble("cantidad");

                        if (idItemObj != null) {
                            pstRestock.setDouble(1, cantidad);
                            pstRestock.setInt(2, ((Number) idItemObj).intValue());
                            pstRestock.executeUpdate();
                        } else if (idComboObj != null) {
                            pstComponentes.setInt(1, ((Number) idComboObj).intValue());
                            try (ResultSet rsComp = pstComponentes.executeQuery()) {
                                while (rsComp.next()) {
                                    double cantidadComponente = rsComp.getDouble("cantidad");
                                    int idItemComponente = rsComp.getInt("id_item");
                                    pstRestock.setDouble(1, cantidadComponente * cantidad);
                                    pstRestock.setInt(2, idItemComponente);
                                    pstRestock.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Error al hacer rollback de la anulación", ex); } }
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public int contarVentasDelDia() throws SQLException {
        String fechaHoy = java.time.LocalDate.now().toString();
        String sql = "SELECT COUNT(*) FROM ventas WHERE fecha LIKE ?" + FILTRO_ACTIVAS;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fechaHoy + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Resta los gastos operativos del día (decisión D4): ganancia = ventas - costo_mercaderia - gastos_del_dia. */
    public double obtenerGananciaEstimadaDelDia() throws SQLException {
        String fechaHoy = java.time.LocalDate.now().toString();
        String sql = "SELECT SUM((d.precio_unitario - i.precio_costo) * d.cantidad) " +
                     "FROM detalles_venta d " +
                     "JOIN items i ON d.id_item = i.id " +
                     "JOIN ventas v ON d.id_venta = v.id " +
                     "WHERE v.fecha LIKE ? AND i.es_servicio = 0 AND d.id_item IS NOT NULL" + FILTRO_ACTIVAS_V;
        double gananciaBruta;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fechaHoy + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                gananciaBruta = rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
        double gastosDelDia = new GastoDAO().sumarGastosEntre(fechaHoy, fechaHoy);
        return gananciaBruta - gastosDelDia;
    }

    public record ItemCritico(String nombre, double stock, String unidad) {}

    public record TopProducto(String nombre, double cantidad, String unidad) {}

    public int contarItemsStockCritico() throws SQLException {
        String sql = "SELECT COUNT(*) FROM items WHERE es_servicio = 0 AND stock <= 5";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Items (no combos) con stock crítico, del más bajo al más alto, hasta {@code limite}. */
    public java.util.List<ItemCritico> obtenerItemsStockCritico(int limite) throws SQLException {
        java.util.List<ItemCritico> resultado = new java.util.ArrayList<>();
        String sql = "SELECT nombre, stock, unidad FROM items WHERE es_servicio = 0 AND stock <= 5 ORDER BY stock ASC LIMIT ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, limite);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new ItemCritico(rs.getString("nombre"), rs.getDouble("stock"), rs.getString("unidad")));
                }
            }
        }
        return resultado;
    }

    public Map<String, Double> obtenerVentasUltimos7Dias() throws SQLException {
        Map<String, Double> resultado = new LinkedHashMap<>();
        String sql = "SELECT DATE(fecha) as dia, SUM(total) as total_dia " +
                     "FROM ventas " +
                     "WHERE DATE(fecha) >= DATE('now', '-6 days')" + FILTRO_ACTIVAS + " " +
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

    public java.util.List<TopProducto> obtenerTop5ProductosMasVendidos() throws SQLException {
        java.util.List<TopProducto> resultado = new java.util.ArrayList<>();
        String sql = "SELECT COALESCE(i.nombre, c.nombre) as nombre, SUM(d.cantidad) as total_vendido, " +
                     "COALESCE(i.unidad, 'u') as unidad " +
                     "FROM detalles_venta d " +
                     "LEFT JOIN items i ON d.id_item = i.id " +
                     "LEFT JOIN combos c ON d.id_combo = c.id " +
                     "JOIN ventas v ON d.id_venta = v.id " +
                     "WHERE 1=1" + FILTRO_ACTIVAS_V + " " +
                     "GROUP BY d.id_item, d.id_combo " +
                     "ORDER BY total_vendido DESC " +
                     "LIMIT 5";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(new TopProducto(rs.getString("nombre"), rs.getDouble("total_vendido"), rs.getString("unidad")));
            }
        }
        return resultado;
    }

    public java.util.List<String[]> obtenerResumenDiario(String desde, String hasta) throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        String sql = "SELECT DATE(fecha) as dia, COUNT(*) as cant, SUM(total) as total_dia " +
                     "FROM ventas WHERE DATE(fecha) BETWEEN ? AND ?" + FILTRO_ACTIVAS + " " +
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
                     "WHERE DATE(v.fecha) BETWEEN ? AND ?" + FILTRO_ACTIVAS_V + " " +
                     "GROUP BY d.id_item, d.id_combo " +
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
                     "WHERE DATE(v.fecha) BETWEEN ? AND ?" + FILTRO_ACTIVAS_V + " " +
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
                    venta.setEstado(rs.getString("estado"));
                    venta.setMotivoAnulacion(rs.getString("motivo_anulacion"));
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

    /** Borra un ticket puntual y sus detalles. El stock NO se restaura (mismo criterio que borrarTodasLasVentas). */
    public boolean borrarVenta(int idVenta) throws SQLException {
        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);
            try (PreparedStatement pstDetalles = conn.prepareStatement("DELETE FROM detalles_venta WHERE id_venta = ?");
                 PreparedStatement pstVenta = conn.prepareStatement("DELETE FROM ventas WHERE id = ?")) {
                pstDetalles.setInt(1, idVenta);
                pstDetalles.executeUpdate();
                pstVenta.setInt(1, idVenta);
                int filas = pstVenta.executeUpdate();
                conn.commit();
                return filas > 0;
            }
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
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

    /** row[4] = mes (YYYY-MM) del primer día con ventas de esa semana — usado para mostrar a qué mes pertenece. */
    public java.util.List<String[]> obtenerVentasPorSemana() throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        String sql = "SELECT strftime('%Y-W%W', fecha) as semana, " +
                     "COUNT(*) as cant, SUM(total) as total_semana, AVG(total) as promedio, " +
                     "strftime('%Y-%m', MIN(fecha)) as mes " +
                     "FROM ventas " + WHERE_ACTIVAS + "GROUP BY semana ORDER BY semana DESC LIMIT 16";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(new String[]{
                    rs.getString("semana"),
                    String.valueOf(rs.getInt("cant")),
                    String.valueOf(rs.getDouble("total_semana")),
                    String.valueOf(rs.getDouble("promedio")),
                    rs.getString("mes")
                });
            }
        }
        java.util.Collections.reverse(resultado);
        return resultado;
    }

    /** Venta local no empujada todavía a Supabase, con sus detalles (sdd/ventas-sync-envio, D5). */
    public record VentaPendiente(int id, String fecha, double total, String estado, List<DetallePendiente> detalles) {}

    /** Línea de una {@link VentaPendiente}. {@code productoCodigo} es null si la línea es un combo. */
    public record DetallePendiente(int id, String productoCodigo, Integer comboId, String descripcion,
                                    double cantidad, double precioUnitario, double subtotal) {}

    /** Ventas con {@code sincronizada_en IS NULL}, incluidas las backdateadas — no hay tratamiento especial. */
    public List<VentaPendiente> listarVentasPendientesDeSync() throws SQLException {
        List<VentaPendiente> resultado = new ArrayList<>();
        String sql = "SELECT id, fecha, total, estado FROM ventas WHERE sincronizada_en IS NULL ORDER BY id ASC";
        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                resultado.add(new VentaPendiente(id, rs.getString("fecha"), rs.getDouble("total"),
                        rs.getString("estado"), listarDetallesPendientes(conn, id)));
            }
        }
        return resultado;
    }

    private List<DetallePendiente> listarDetallesPendientes(Connection conn, int idVenta) throws SQLException {
        List<DetallePendiente> detalles = new ArrayList<>();
        String sql = "SELECT d.id, d.id_combo, d.cantidad, d.precio_unitario, d.subtotal, "
                + "i.codigo as producto_codigo, COALESCE(i.nombre, c.nombre) as descripcion "
                + "FROM detalles_venta d "
                + "LEFT JOIN items i ON d.id_item = i.id "
                + "LEFT JOIN combos c ON d.id_combo = c.id "
                + "WHERE d.id_venta = ? ORDER BY d.id ASC";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idVenta);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Object idComboObj = rs.getObject("id_combo");
                    detalles.add(new DetallePendiente(
                            rs.getInt("id"),
                            rs.getString("producto_codigo"),
                            idComboObj != null ? ((Number) idComboObj).intValue() : null,
                            rs.getString("descripcion"),
                            rs.getDouble("cantidad"),
                            rs.getDouble("precio_unitario"),
                            rs.getDouble("subtotal")));
                }
            }
        }
        return detalles;
    }

    /** Marca las ventas indicadas como sincronizadas con Supabase, en una única transacción. */
    public void marcarVentasSincronizadas(List<Integer> idsVenta) throws SQLException {
        if (idsVenta == null || idsVenta.isEmpty()) return;

        String syncedAt = Instant.now().toString();
        String sql = "UPDATE ventas SET sincronizada_en = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            conn.setAutoCommit(false);
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                for (int id : idsVenta) {
                    pst.setString(1, syncedAt);
                    pst.setInt(2, id);
                    pst.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Error al hacer rollback al marcar ventas sincronizadas", ex); } }
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }

    public java.util.List<String[]> obtenerVentasPorMes() throws SQLException {
        java.util.List<String[]> resultado = new java.util.ArrayList<>();
        // Sin JOIN a detalles_venta: un LEFT JOIN acá duplica cada fila de
        // "ventas" por cada línea de producto del ticket, inflando
        // COUNT(*) y SUM(total) en cualquier venta con más de un ítem.
        String sql = "SELECT strftime('%Y-%m', fecha) as mes, " +
                     "COUNT(*) as cant, SUM(total) as total_mes " +
                     "FROM ventas " + WHERE_ACTIVAS +
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
                     "JOIN ventas v ON da.id_venta = v.id " +
                     "WHERE da.id_item IS NOT NULL AND db.id_item IS NOT NULL" + FILTRO_ACTIVAS_V + " " +
                     "GROUP BY da.id_item, db.id_item " +
                     "HAVING frec >= 2 " +
                     "ORDER BY frec DESC LIMIT 15";
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
                     "FROM ventas " + WHERE_ACTIVAS + "GROUP BY hora ORDER BY hora ASC";
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
                     "FROM ventas " + WHERE_ACTIVAS + "GROUP BY dia, hora ORDER BY dia, hora";
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
