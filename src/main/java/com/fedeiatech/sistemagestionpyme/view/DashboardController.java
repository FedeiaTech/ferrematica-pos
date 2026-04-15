package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.service.IFiscalProvider;
import com.fedeiatech.sistemagestionpyme.service.LicenseService;
import com.fedeiatech.sistemagestionpyme.service.MockFiscalProvider;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class DashboardController implements Initializable {

    @FXML private Label lblEstadoFiscal;
    @FXML private Label lblVentasDia;
    @FXML private Label lblContadorVentas;
    @FXML private Label lblGananciaDia;
    @FXML private Label lblStockCritico;
    @FXML private BarChart<String, Number> chartVentas7Dias;
    @FXML private PieChart chartTop5;

    private IFiscalProvider fiscalProvider;
    private VentaDAO ventaDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fiscalProvider = new MockFiscalProvider();
        ventaDAO = new VentaDAO();

        verificarEstadoFiscal();
        cargarMetricas();
    }

    private void cargarMetricas() {
        try {
            double totalHoy = ventaDAO.sumarVentasDelDia();
            lblVentasDia.setText(String.format("ARS %.2f", totalHoy));

            int cantVentas = ventaDAO.contarVentasDelDia();
            lblContadorVentas.setText(cantVentas + " transacciones hoy");

            double ganancia = ventaDAO.obtenerGananciaEstimadaDelDia();
            lblGananciaDia.setText(String.format("ARS %.2f", ganancia));

            int critico = ventaDAO.contarItemsStockCritico();
            lblStockCritico.setText(String.valueOf(critico));
            lblStockCritico.setTextFill(
                critico == 0 ? Color.web("#27ae60") :
                critico <= 3 ? Color.web("#f39c12") : Color.web("#e74c3c")
            );

            cargarGraficoVentas7Dias();
            cargarGraficoTop5();

        } catch (SQLException e) {
            lblVentasDia.setText("Error");
            e.printStackTrace();
        }
    }

    private void cargarGraficoVentas7Dias() throws SQLException {
        Map<String, Double> datos = ventaDAO.obtenerVentasUltimos7Dias();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Map.Entry<String, Double> entry : datos.entrySet()) {
            String dia = entry.getKey().substring(5);
            serie.getData().add(new XYChart.Data<>(dia, entry.getValue()));
        }
        chartVentas7Dias.getData().clear();
        chartVentas7Dias.getData().add(serie);
    }

    private void cargarGraficoTop5() throws SQLException {
        Map<String, Double> datos = ventaDAO.obtenerTop5ProductosMasVendidos();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : datos.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        chartTop5.setData(pieData);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/inventory_view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Gestión de Inventario");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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
        boolean nuevoEstado = !LicenseService.esPremium();
        LicenseService.setPremium(nuevoEstado);
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