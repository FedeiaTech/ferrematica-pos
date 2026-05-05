package com.fedeiatech.sistemagestionpyme.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSeeder {

    public static void sembrarDemoSiVacio() {
        try (Connection conn = ConexionDB.getConexion()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM items")) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
            conn.setAutoCommit(false);
            try {
                insertarItems(conn);
                insertarVentas(conn);
                conn.commit();
                System.out.println("[DataSeeder] Datos demo de kiosco cargados correctamente.");
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[DataSeeder] Error al cargar datos demo: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[DataSeeder] No se pudo conectar: " + e.getMessage());
        }
    }

    private static void insertarItems(Connection conn) throws SQLException {
        String sql = "INSERT INTO items (codigo, nombre, descripcion, precio_costo, precio_venta, stock, es_servicio, unidad) VALUES (?, ?, ?, ?, ?, ?, 0, ?)";
        Object[][] items = {
            {"CC500", "Coca-Cola 500ml",       "Bebida gaseosa cola",      900.0,  1500.0, 24.0,  "u"},
            {"SP500", "Sprite 500ml",           "Bebida gaseosa lima-limón",850.0,  1400.0, 18.0,  "u"},
            {"AGUA",  "Agua Mineral",           "Sin gas 500ml",            450.0,   800.0, 30.0,  "u"},
            {"ALFM",  "Alfajor Milka",          "Alfajor de chocolate",     700.0,  1200.0, 20.0,  "u"},
            {"CHIC",  "Chicles Beldent",        "Pastillas menta",          350.0,   600.0, 50.0,  "u"},
            {"CIGA",  "Cigarrillos Marlboro",   "Atado x20 unidades",      3800.0,  4500.0, 15.0,  "u"},
            {"PAPA",  "Papas Fritas Lay's",     "Bolsa 55g",               1300.0,  2000.0, 12.0,  "u"},
            {"MANI",  "Maní Tostado",           "A granel, precio por kg", 5000.0,  8000.0,  3.5,  "kg"},
            {"CARA",  "Caramelos Surtidos",     "Precio por gramo",           6.0,    10.0, 450.0, "g"},
            {"HELA",  "Helado Palito Frutilla", "Palito de agua frutilla",  800.0,  1500.0, 12.0,  "u"},
        };
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            for (Object[] item : items) {
                pst.setString(1, (String) item[0]);
                pst.setString(2, (String) item[1]);
                pst.setString(3, (String) item[2]);
                pst.setDouble(4, (double) item[3]);
                pst.setDouble(5, (double) item[4]);
                pst.setDouble(6, (double) item[5]);
                pst.setString(7, (String) item[6]);
                pst.executeUpdate();
            }
        }
    }

    private static void insertarVentas(Connection conn) throws SQLException {
        // Semana 1 — 1 al 7 de abril
        int v1 = venta(conn, "2026-04-01T09:15:00");
        detalle(conn, v1, 1, 1,   1500); detalle(conn, v1, 4, 1, 1200); total(conn, v1);

        int v2 = venta(conn, "2026-04-01T14:30:00");
        detalle(conn, v2, 6, 1,   4500); detalle(conn, v2, 3, 1,  800); total(conn, v2);

        int v3 = venta(conn, "2026-04-01T18:45:00");
        detalle(conn, v3, 7, 1,   2000); detalle(conn, v3, 2, 1, 1400); total(conn, v3);

        int v4 = venta(conn, "2026-04-02T10:00:00");
        detalle(conn, v4, 5, 2,    600); detalle(conn, v4, 1, 2, 1500); total(conn, v4);

        int v5 = venta(conn, "2026-04-02T16:20:00");
        detalle(conn, v5, 10, 1, 1500); detalle(conn, v5, 3, 1,  800); total(conn, v5);

        int v6 = venta(conn, "2026-04-03T11:30:00");
        detalle(conn, v6, 6, 1,   4500); total(conn, v6);

        int v7 = venta(conn, "2026-04-04T09:00:00");
        detalle(conn, v7, 1, 1,   1500); detalle(conn, v7, 4, 2, 1200); total(conn, v7);

        int v8 = venta(conn, "2026-04-04T15:45:00");
        detalle(conn, v8, 7, 1,   2000); detalle(conn, v8, 2, 1, 1400); detalle(conn, v8, 5, 1, 600); total(conn, v8);

        int v9 = venta(conn, "2026-04-05T10:30:00");
        detalle(conn, v9, 8, 0.5, 8000); total(conn, v9);

        int v10 = venta(conn, "2026-04-05T17:00:00");
        detalle(conn, v10, 1, 1,  1500); detalle(conn, v10, 10, 2, 1500); total(conn, v10);

        int v11 = venta(conn, "2026-04-06T08:45:00");
        detalle(conn, v11, 6, 2,  4500); total(conn, v11);

        int v12 = venta(conn, "2026-04-07T11:15:00");
        detalle(conn, v12, 3, 2,   800); detalle(conn, v12, 4, 1, 1200); total(conn, v12);

        // Semana 2 — 8 al 14 de abril
        int v13 = venta(conn, "2026-04-08T10:00:00");
        detalle(conn, v13, 1, 2,  1500); detalle(conn, v13, 2, 1, 1400); total(conn, v13);

        int v14 = venta(conn, "2026-04-08T14:30:00");
        detalle(conn, v14, 5, 3,   600); detalle(conn, v14, 7, 1, 2000); total(conn, v14);

        int v15 = venta(conn, "2026-04-09T09:15:00");
        detalle(conn, v15, 6, 1,  4500); detalle(conn, v15, 1, 1, 1500); total(conn, v15);

        int v16 = venta(conn, "2026-04-09T16:00:00");
        detalle(conn, v16, 9, 100, 10); total(conn, v16);

        int v17 = venta(conn, "2026-04-10T11:30:00");
        detalle(conn, v17, 10, 3, 1500); detalle(conn, v17, 4, 1, 1200); total(conn, v17);

        int v18 = venta(conn, "2026-04-10T18:00:00");
        detalle(conn, v18, 8, 1,  8000); detalle(conn, v18, 3, 1,  800); total(conn, v18);

        int v19 = venta(conn, "2026-04-11T10:00:00");
        detalle(conn, v19, 2, 2,  1400); detalle(conn, v19, 7, 1, 2000); total(conn, v19);

        int v20 = venta(conn, "2026-04-11T15:30:00");
        detalle(conn, v20, 1, 1,  1500); detalle(conn, v20, 6, 1, 4500); total(conn, v20);

        int v21 = venta(conn, "2026-04-12T09:30:00");
        detalle(conn, v21, 10, 2, 1500); detalle(conn, v21, 5, 2,  600); total(conn, v21);

        int v22 = venta(conn, "2026-04-12T14:00:00");
        detalle(conn, v22, 4, 2,  1200); detalle(conn, v22, 3, 2,  800); total(conn, v22);

        int v23 = venta(conn, "2026-04-13T10:15:00");
        detalle(conn, v23, 6, 1,  4500); detalle(conn, v23, 7, 1, 2000); total(conn, v23);

        int v24 = venta(conn, "2026-04-14T09:00:00");
        detalle(conn, v24, 1, 3,  1500); detalle(conn, v24, 2, 1, 1400); total(conn, v24);

        int v25 = venta(conn, "2026-04-14T17:30:00");
        detalle(conn, v25, 9, 200, 10); detalle(conn, v25, 5, 2,  600); total(conn, v25);
    }

    private static int venta(Connection conn, String fecha) throws SQLException {
        String sql = "INSERT INTO ventas (fecha, total) VALUES (?, 0)";
        try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, fecha);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void detalle(Connection conn, int idVenta, int idItem, double cantidad, double precio) throws SQLException {
        String sql = "INSERT INTO detalles_venta (id_venta, id_item, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idVenta);
            pst.setInt(2, idItem);
            pst.setDouble(3, cantidad);
            pst.setDouble(4, precio);
            pst.setDouble(5, cantidad * precio);
            pst.executeUpdate();
        }
    }

    private static void total(Connection conn, int idVenta) throws SQLException {
        String sql = "UPDATE ventas SET total = (SELECT SUM(subtotal) FROM detalles_venta WHERE id_venta = ?) WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idVenta);
            pst.setInt(2, idVenta);
            pst.executeUpdate();
        }
    }
}