package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.service.IFiscalProvider;
import com.fedeiatech.sistemagestionpyme.service.MockFiscalProvider;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import com.fedeiatech.sistemagestionpyme.service.LeerMeService;
import com.fedeiatech.sistemagestionpyme.service.SupabaseSyncService;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import com.fedeiatech.sistemagestionpyme.view.util.AlertUtil;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DashboardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    @FXML private AnchorPane rootPane;
    @FXML private Label lblEstadoFiscal;
    @FXML private Label lblEstadoSync;
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
        actualizarEstadoSync();
        cargarMetricas();
    }

    private void aplicarRestriccionesPorRol() {
        Usuario usuario = SessionService.getInstance().getUsuarioActivo();
        if (usuario == null) return;

        lblUsuario.setText(usuario.getNombre() + "  ·  " + usuario.getRol());

        boolean esAdmin = usuario.esAdmin();

        btnInventario.setDisable(!esAdmin);
        btnConfiguracion.setDisable(!esAdmin);
        btnUsuarios.setVisible(esAdmin);
        btnUsuarios.setManaged(esAdmin);
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
            LOGGER.log(Level.SEVERE, "Error al cargar métricas del dashboard", e);
            AlertUtil.mostrarError("Error al cargar dashboard", "No se pudieron cargar las métricas: " + e.getMessage());
        }
        try { cargarGraficoVentas7Dias(); } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar gráfico de 7 días", e);
            AlertUtil.mostrarError("Error al cargar gráfico", "No se pudo cargar el gráfico de ventas: " + e.getMessage());
        }
        try { cargarGraficoTop5(); } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar gráfico top 5", e);
            AlertUtil.mostrarError("Error al cargar gráfico", "No se pudo cargar el gráfico de productos: " + e.getMessage());
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
        ((CategoryAxis) chartVentas7Dias.getXAxis()).getCategories().clear();
        chartVentas7Dias.getData().add(serie);
    }

    private void cargarGraficoTop5() throws SQLException {
        Map<String, Double> datos = ventaDAO.obtenerTop5ProductosMasVendidos();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : datos.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        chartTop5.setData(pieData);
        chartTop5.layout();
    }

    private void verificarEstadoFiscal() {
        lblEstadoFiscal.setText("ARCA (sin configurar)");
        lblEstadoFiscal.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-background-radius: 15; -fx-padding: 5 15;");
    }

    private void actualizarEstadoSync() {
        if (lblEstadoSync == null) return;
        try {
            Configuracion config = new ConfiguracionDAO().obtenerConfiguracion();
            boolean habilitado = config != null && config.isSupabaseSyncHabilitado();
            Instant ultimaOk = SupabaseSyncService.getInstance().ultimaSincronizacionExitosaEn();

            if (!habilitado) {
                lblEstadoSync.setText("Sync (deshabilitado)");
                lblEstadoSync.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-background-radius: 15; -fx-padding: 5 15;");
            } else if (ultimaOk == null) {
                lblEstadoSync.setText("Sync (nunca sincronizado)");
                lblEstadoSync.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;");
            } else {
                String hora = DateTimeFormatter.ofPattern("HH:mm")
                        .withZone(ZoneId.systemDefault())
                        .format(ultimaOk);
                lblEstadoSync.setText("Sync ✓ " + hora);
                lblEstadoSync.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo determinar el estado de sincronización con Supabase", e);
        }
    }

    @FXML
    void abrirInventario(ActionEvent event) {
        abrirVentana("/inventory_view.fxml", "Gestión de Inventario", false, this::cargarMetricas);
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
            stage.initOwner(rootPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setOnHidden(e -> cargarMetricas());
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir el POS", e);
        }
    }

    @FXML
    void abrirReportes(ActionEvent event) {
        abrirVentana("/reports_view.fxml", "Reportes Avanzados", false);
    }

    @FXML
    void abrirConfiguracion(ActionEvent event) {
        abrirVentana("/config_view.fxml", "Configuración de Empresa", false, () -> {
            aplicarRestriccionesPorRol();
            actualizarEstadoSync();
            cargarMetricas();
        });
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
    void abrirLeerMe(ActionEvent event) {
        LeerMeService.abrirLeerMe();
    }

    @FXML
    void abrirAcercaDe(ActionEvent event) {
        Alert dlg = new Alert(Alert.AlertType.INFORMATION);
        dlg.setTitle("Acerca de");
        dlg.setHeaderText("Sistema de Gestión PyME  —  v1.0.0");
        dlg.setContentText(
            "Desarrollado por Federico Iacono\n" +
            "IATech — Soluciones de software para PyMEs argentinas\n\n" +
            "© 2026 IATech. Todos los derechos reservados.\n\n" +
            "Contacto: iaconofede@gmail.com\n" +
            "Instagram: iatech.dev"
        );
        dlg.showAndWait();
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
            stage.setTitle("Sistema FedeiaTech - Pyme v1.0.0");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cerrar sesión", e);
        }
    }

    private void abrirVentana(String fxmlPath, String titulo, boolean maximizar) {
        abrirVentana(fxmlPath, titulo, maximizar, null);
    }

    private void abrirVentana(String fxmlPath, String titulo, boolean maximizar, Runnable alCerrar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(root));
            if (maximizar) stage.setMaximized(true);
            stage.initOwner(rootPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            if (alCerrar != null) stage.setOnHidden(e -> alCerrar.run());
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir ventana: " + fxmlPath, e);
        }
    }
}
