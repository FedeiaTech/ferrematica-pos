package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThemeService {

    private static final Logger LOGGER = Logger.getLogger(ThemeService.class.getName());

    public static final String[] COLORES = {"#ffffff", "#c9cfd4", "#c9d3cd", "#b3cad6", "#cbafd4", "#d6c9b3"};
    public static final String[] NOMBRES  = {"Blanco", "Azul claro", "Verde claro", "Azul", "Lavanda", "Crema"};

    private static ThemeService instancia;
    private String colorActual = COLORES[0];

    private ThemeService() {
        cargarDesdeBD();
    }

    public static ThemeService getInstance() {
        if (instancia == null) instancia = new ThemeService();
        return instancia;
    }

    private void cargarDesdeBD() {
        String sql = "SELECT color_tema FROM configuracion WHERE id = 1";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                String color = rs.getString("color_tema");
                if (color != null && !color.isBlank()) colorActual = color;
            }
        } catch (SQLException e) { /* usar default */ }
    }

    public String getColor() { return colorActual; }

    public String getBgStyle() { return "-fx-background-color: " + colorActual + ";"; }

    public void setColor(String color) {
        colorActual = color;
        String sql = "UPDATE configuracion SET color_tema = ? WHERE id = 1";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, color);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar el color de tema", e);
        }
    }
}
