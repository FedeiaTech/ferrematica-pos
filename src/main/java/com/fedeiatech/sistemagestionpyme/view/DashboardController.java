package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.service.IFiscalProvider;
import com.fedeiatech.sistemagestionpyme.service.LicenseService;
import com.fedeiatech.sistemagestionpyme.service.MockFiscalProvider;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DashboardController implements Initializable {

    @FXML
    private Label lblEstadoFiscal;
    @FXML 
    private Label lblVentasDia;

    private IFiscalProvider fiscalProvider;
    private VentaDAO ventaDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inicializamos el proveedor fiscal (Simulado por ahora)
        fiscalProvider = new MockFiscalProvider();
        ventaDAO = new VentaDAO();
        
        verificarEstadoFiscal();
        cargarMetricas();
    }
    
    private void cargarMetricas() {
        try {
            double totalHoy = ventaDAO.sumarVentasDelDia();
            // Formateamos a 2 decimales
            lblVentasDia.setText(String.format("ARS %.2f", totalHoy));
        } catch (SQLException e) {
            lblVentasDia.setText("Error");
            e.printStackTrace();
        }
    }

    private void verificarEstadoFiscal() {
        if (fiscalProvider != null && fiscalProvider.isServicioDisponible()) {
            lblEstadoFiscal.setText("🟢 ARCA Online");
            lblEstadoFiscal.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;");
        } else {
            lblEstadoFiscal.setText("🔴 Sin Conexión");
            lblEstadoFiscal.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;");
        }
    }

    @FXML
    void abrirInventario(ActionEvent event) {
        try {
            // CAMBIO: Ahora apunta a inventory_view.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/inventory_view.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Gestión de Inventario");
            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al abrir inventario: " + e.getMessage());
        }
    }
    
    @FXML
    void abrirPOS(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pos_view.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Punto de Venta");
            stage.setMaximized(true);
            stage.setScene(new Scene(root));
            
            stage.setOnHidden(e -> cargarMetricas()); 
            
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    void abrirReportes(ActionEvent event) {
        // VERIFICACIÓN DE LICENCIA (FEATURE FLAG)
        if (LicenseService.permiteReportes()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/reports_view.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setTitle("Reportes Avanzados (PRO)");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // MENSAJE DE UPSELL (VENTA)
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Característica Premium");
            alert.setHeaderText("¡Desbloquea los Reportes!");
            alert.setContentText("Esta función es exclusiva de la versión PRO.\n\n"
                    + "Adquiere tu licencia para acceder al historial completo, "
                    + "exportación a Excel y métricas avanzadas.");
            alert.showAndWait();
        }
    }
    
    @FXML
    void cambiarModoDev(ActionEvent event) {
        // 1. Invertir el estado actual
        boolean nuevoEstado = !LicenseService.esPremium();
        LicenseService.setPremium(nuevoEstado);
        
        // 2. Avisar al desarrollador (Tú)
        String modo = nuevoEstado ? "PREMIUM (PRO)" : "FREE (Community)";
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Modo Desarrollador");
        alert.setHeaderText("Licencia Cambiada");
        alert.setContentText("El sistema ahora simula ser versión: " + modo + "\n\nPrueba los botones bloqueados ahora.");
        alert.showAndWait();
    }
    
    @FXML
    void abrirConfiguracion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/config_view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Configuración de Empresa");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}