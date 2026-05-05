package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.service.IFiscalProvider;
import com.fedeiatech.sistemagestionpyme.service.LicenseService;
import com.fedeiatech.sistemagestionpyme.service.MockFiscalProvider;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class DashboardController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private Label lblEstadoFiscal;
    @FXML private Label lblVentasDia;
    @FXML private Label lblContadorVentas;
    @FXML private Label lblGananciaDia;
    @FXML private Label lblStockCritico;
    @FXML private Label lblUsuario;
    @FXML private BarChart<String, Number> chartVentas7Dias;
    @FXML private PieChart chartTop5;
    @FXML private Button btnInventario;
    @FXML private Button btnConfiguracion;
    @FXML private Button btnReportes;
    @FXML private Button btnUsuarios;
    @FXML private Button btnTema0;
    @FXML private Button btnTema1;
    @FXML private Button btnTema2;
    @FXML private Button btnTema3;
    @FXML private Button btnTema4;
    @FXML private Button btnTema5;
    @FXML private Button btnPremium;
    @FXML private Button btnEstadisticas;

    private IFiscalProvider fiscalProvider;
    private VentaDAO ventaDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fiscalProvider = new MockFiscalProvider();
        ventaDAO = new VentaDAO();

        rootPane.setStyle(ThemeService.getInstance().getBgStyle());

        Button[] botonesTema = {btnTema0, btnTema1, btnTema2, btnTema3, btnTema4, btnTema5};
        for (int i = 0; i < botonesTema.length; i++) {
            botonesTema[i].setUserData(i);
        }

        aplicarRestriccionesPorRol();
        verificarEstadoFiscal();
        cargarMetricas();
    }

    private void aplicarRestriccionesPorRol() {
        Usuario usuario = SessionService.getInstance().getUsuarioActivo();
        if (usuario == null) return;

        lblUsuario.setText(usuario.getNombre() + "  ·  " + usuario.getRol());

        boolean esAdmin = usuario.esAdmin();
        boolean esPremium = LicenseService.esPremium();

        btnInventario.setDisable(!esAdmin);
        btnConfiguracion.setDisable(!esAdmin);
        btnReportes.setDisable(!esPremium);
        if (btnEstadisticas != null) btnEstadisticas.setDisable(!esPremium);
        btnUsuarios.setVisible(esAdmin && esPremium);
        btnUsuarios.setManaged(esAdmin && esPremium);

        if (btnPremium != null) {
            if (esPremium) {
                btnPremium.setText("PREMIUM ✓");
                btnPremium.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 5 12;");
            } else {
                btnPremium.setText("PREMIUM 🔒");
                btnPremium.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 11; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 5 12;");
            }
        }
    }

    @FXML
    void abrirDialogoPremium(ActionEvent event) {
        if (LicenseService.esPremium()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Premium activo");
            a.setContentText("El modo Premium ya está activado en esta instalación.");
            a.showAndWait();
            return;
        }

        PasswordField pf = new PasswordField();
        pf.setPromptText("Clave de activación");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Activar Premium");
        dialog.setHeaderText("Ingresá la clave de activación");
        dialog.getDialogPane().setContent(pf);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (LicenseService.verificarYDesbloquear(pf.getText())) {
                    aplicarRestriccionesPorRol();
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Activación exitosa");
                    ok.setContentText("¡Modo Premium activado correctamente!");
                    ok.showAndWait();
                } else {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Clave incorrecta");
                    err.setContentText("La clave de activación no es válida.");
                    err.showAndWait();
                }
            }
        });
    }

    private void cargarMetricas() {
        try {
            double totalHoy = ventaDAO.sumarVentasDelDia();
            lblVentasDia.setText(String.format("ARS %.2f", totalHoy));

            int cantVentas = ventaDAO.contarVentasDelDia();
            lblContadorVentas.setText(cantVentas + " transacciones hoy");

            double ganancia;
            try {
                Configuracion cfg = new ConfiguracionDAO().obtenerConfiguracion();
                if (cfg != null && cfg.getMargenGananciaPct() > 0) {
                    ganancia = totalHoy * (cfg.getMargenGananciaPct() / 100.0);
                } else {
                    ganancia = ventaDAO.obtenerGananciaEstimadaDelDia();
                }
            } catch (Exception ex) {
                ganancia = ventaDAO.obtenerGananciaEstimadaDelDia();
            }
            lblGananciaDia.setText(String.format("ARS %.2f", ganancia));

            int critico = ventaDAO.contarItemsStockCritico();
            lblStockCritico.setText(String.valueOf(critico));
            lblStockCritico.setTextFill(
                critico == 0 ? Color.web("#27ae60") :
                critico <= 3 ? Color.web("#f39c12") : Color.web("#e74c3c")
            );

        } catch (SQLException e) {
            lblVentasDia.setText("Error");
            e.printStackTrace();
        }
        try { cargarGraficoVentas7Dias(); } catch (Exception e) { e.printStackTrace(); }
        try { cargarGraficoTop5(); } catch (Exception e) { e.printStackTrace(); }
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
        lblEstadoFiscal.setText("ARCA (sin configurar)");
        lblEstadoFiscal.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-background-radius: 15; -fx-padding: 5 15;");
    }

    @FXML
    void abrirInventario(ActionEvent event) {
        abrirVentana("/inventory_view.fxml", "Gestión de Inventario", false);
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
        abrirVentana("/reports_view.fxml", "Reportes Avanzados", false);
    }

    @FXML
    void abrirConfiguracion(ActionEvent event) {
        abrirVentana("/config_view.fxml", "Configuración de Empresa", false);
    }

    @FXML
    void abrirUsuarios(ActionEvent event) {
        abrirVentana("/admin_usuarios_view.fxml", "Gestión de Usuarios", false);
    }

    @FXML
    void abrirEstadisticasDashboard(ActionEvent event) {
        abrirVentana("/stats_view.fxml", "Estadísticas Avanzadas", false);
    }

    @FXML
    void cambiarTema(ActionEvent event) {
        int idx = (int) ((Button) event.getSource()).getUserData();
        ThemeService.getInstance().setColor(ThemeService.COLORES[idx]);
        rootPane.setStyle(ThemeService.getInstance().getBgStyle());
    }

    @FXML
    void cerrarSesion(ActionEvent event) {
        try {
            SessionService.getInstance().cerrarSesion();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login_view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(false);
            stage.setTitle("Sistema FedeiaTech - Pyme v0.7");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirVentana(String fxmlPath, String titulo, boolean maximizar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(root));
            if (maximizar) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
