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
import com.fedeiatech.sistemagestionpyme.service.SyncBloqueadoException;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
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
    @FXML private VBox vboxItemsCriticos;
    @FXML private Label lblUsuario;
    @FXML private Label lblFechaHora;
    @FXML private Label lblSaludo;
    @FXML private BarChart<String, Number> chartVentas7Dias;
    @FXML private PieChart chartTop5;
    @FXML private TabPane tabPaneDashboard;
    @FXML private Tab tabBalances;
    @FXML private BalancesController balancesController;
    @FXML private Button btnInventario;
    @FXML private Button btnCompras;
    @FXML private Button btnGastos;
    @FXML private Button btnConfiguracion;
    @FXML private Button btnReportes;
    @FXML private Button btnUsuarios;
    @FXML private Button btnTema0;
    @FXML private Button btnTema1;
    @FXML private Button btnTema2;
    @FXML private Button btnTema3;
    @FXML private Button btnTema4;
    @FXML private Button btnTema5;
    @FXML private Button btnTema6;
    @FXML private Button btnEstadisticas;

    private IFiscalProvider fiscalProvider;
    private VentaDAO ventaDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fiscalProvider = new MockFiscalProvider();
        ventaDAO = new VentaDAO();

        rootPane.setStyle(ThemeService.getInstance().getBgStyle());

        Button[] botonesTema = {btnTema0, btnTema1, btnTema2, btnTema3, btnTema4, btnTema5, btnTema6};
        for (int i = 0; i < botonesTema.length; i++) {
            botonesTema[i].setUserData(i);
        }

        aplicarRestriccionesPorRol();
        verificarEstadoFiscal();
        actualizarEstadoSync();
        cargarMetricas();
        iniciarRelojFechaHora();

        if (lblEstadoSync != null) {
            lblEstadoSync.setOnMouseClicked(e -> sincronizarManualDesdeIndicador());
        }

        tabBalances.setOnSelectionChanged(e -> {
            if (tabBalances.isSelected()) balancesController.cargar();
        });
    }

    private void aplicarRestriccionesPorRol() {
        Usuario usuario = SessionService.getInstance().getUsuarioActivo();
        if (usuario == null) return;

        lblUsuario.setText(usuario.getNombre() + "  ·  " + usuario.getRol());

        boolean esAdmin = usuario.esAdmin();

        btnInventario.setDisable(!esAdmin);
        btnCompras.setDisable(!esAdmin);
        btnGastos.setDisable(!esAdmin);
        tabBalances.setDisable(!esAdmin);
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

            double ganancia = ventaDAO.obtenerGananciaEstimadaDelDia();
            lblGananciaDia.setText(String.format("ARS %.2f", ganancia));

            int critico = ventaDAO.contarItemsStockCritico();
            lblStockCritico.setText(String.valueOf(critico));
            lblStockCritico.setTextFill(
                critico == 0 ? Color.web("#27ae60") :
                critico <= 3 ? Color.web("#f39c12") : Color.web("#e74c3c")
            );
            actualizarListaStockCritico();

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
        java.time.LocalDate hoyFecha = java.time.LocalDate.now();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate fecha = hoyFecha.minusDays(i);
            String clave = fecha.toString();
            boolean esHoy = i == 0;
            String etiqueta = esHoy ? clave.substring(5) + " (Hoy)" : clave.substring(5);
            double valor = datos.getOrDefault(clave, 0.0);
            XYChart.Data<String, Number> dato = new XYChart.Data<>(etiqueta, valor);
            String colorBarra = esHoy ? "#1f618d" : "#5dade2";
            dato.nodeProperty().addListener((obs, nodoAnterior, nodo) -> {
                if (nodo != null) nodo.setStyle("-fx-bar-fill: " + colorBarra + ";");
            });
            serie.getData().add(dato);
        }
        chartVentas7Dias.getData().clear();
        ((CategoryAxis) chartVentas7Dias.getXAxis()).getCategories().clear();
        chartVentas7Dias.getData().add(serie);
    }

    private void cargarGraficoTop5() throws SQLException {
        java.util.List<VentaDAO.TopProducto> datos = ventaDAO.obtenerTop5ProductosMasVendidos();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        Map<String, String> etiquetaPorNombre = new java.util.LinkedHashMap<>();
        for (VentaDAO.TopProducto p : datos) {
            pieData.add(new PieChart.Data(p.nombre(), p.cantidad()));
            etiquetaPorNombre.put(p.nombre(), formatearCantidad(p.cantidad()) + " " + p.unidad());
        }
        chartTop5.setData(pieData);
        chartTop5.layout();

        javafx.application.Platform.runLater(() -> {
            for (javafx.scene.Node nodo : chartTop5.lookupAll(".chart-pie-label")) {
                if (nodo instanceof Text texto) {
                    String etiqueta = etiquetaPorNombre.get(texto.getText());
                    if (etiqueta != null) texto.setText(etiqueta);
                }
            }
        });
    }

    private String formatearCantidad(double cantidad) {
        return cantidad % 1 == 0 ? String.valueOf((int) cantidad) : String.valueOf(cantidad);
    }

    private void actualizarListaStockCritico() throws SQLException {
        java.util.List<VentaDAO.ItemCritico> items = ventaDAO.obtenerItemsStockCritico(5);
        vboxItemsCriticos.getChildren().clear();
        for (VentaDAO.ItemCritico item : items) {
            Label fila = new Label("• " + item.nombre() + " — " + formatearCantidad(item.stock()) + " " + item.unidad());
            fila.setTextFill(Color.web(item.stock() == 0 ? "#e74c3c" : "#f39c12"));
            fila.setStyle("-fx-font-size: 11; -fx-font-weight: bold;");
            vboxItemsCriticos.getChildren().add(fila);
        }
    }

    private void iniciarRelojFechaHora() {
        if (lblFechaHora == null) return;
        actualizarFechaHora();
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> actualizarFechaHora()));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }

    private void actualizarFechaHora() {
        LocalDateTime ahora = LocalDateTime.now();
        String fecha = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora = ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String saludo = ahora.getHour() >= 12 ? "Buenas tardes" : "Buenos días";
        Usuario usuario = SessionService.getInstance().getUsuarioActivo();
        String nombre = usuario != null ? usuario.getNombre() : "";
        if (lblSaludo != null) lblSaludo.setText(saludo + ", " + nombre);
        lblFechaHora.setText(fecha + " · " + hora);
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

            boolean esAdmin = SessionService.getInstance().esAdmin();
            String cursor = esAdmin && habilitado ? "-fx-cursor: hand;" : "";

            if (!habilitado) {
                lblEstadoSync.setText("Sync (deshabilitado)");
                lblEstadoSync.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-background-radius: 15; -fx-padding: 5 15;" + cursor);
            } else if (ultimaOk == null) {
                lblEstadoSync.setText("Sync (nunca sincronizado)");
                lblEstadoSync.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;" + cursor);
            } else {
                String hora = DateTimeFormatter.ofPattern("HH:mm")
                        .withZone(ZoneId.systemDefault())
                        .format(ultimaOk);
                lblEstadoSync.setText("Sync ✓ " + hora);
                lblEstadoSync.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;" + cursor);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo determinar el estado de sincronización con Supabase", e);
        }
    }

    /** Click en el indicador "Sync" del dashboard — dispara una sincronización manual (solo ADMIN, y solo si ya está habilitada desde Configuración). */
    private void sincronizarManualDesdeIndicador() {
        if (!SessionService.getInstance().esAdmin()) return;

        try {
            Configuracion config = new ConfiguracionDAO().obtenerConfiguracion();
            if (config == null || !config.isSupabaseSyncHabilitado()) {
                AlertUtil.mostrarInfo("Sincronización deshabilitada",
                        "Habilitá la sincronización con Supabase desde Configuración → Sincronización antes de sincronizar manualmente.");
                return;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al leer configuración antes de sincronizar", e);
            return;
        }

        lblEstadoSync.setText("Sincronizando...");
        lblEstadoSync.setDisable(true);
        new Thread(() -> {
            try {
                SupabaseSyncService.getInstance().sincronizar();
                javafx.application.Platform.runLater(() -> {
                    lblEstadoSync.setDisable(false);
                    actualizarEstadoSync();
                });
            } catch (SyncBloqueadoException e) {
                javafx.application.Platform.runLater(() -> {
                    lblEstadoSync.setDisable(false);
                    actualizarEstadoSync();
                    AlertUtil.mostrarInfo("Sincronización bloqueada", e.getMessage());
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al sincronizar con Supabase", e);
                javafx.application.Platform.runLater(() -> {
                    lblEstadoSync.setDisable(false);
                    actualizarEstadoSync();
                    AlertUtil.mostrarInfo("Error", "No se pudo sincronizar con Supabase: " + e.getMessage());
                });
            }
        }, "supabase-sync-manual-dashboard").start();
    }

    @FXML
    void abrirInventario(ActionEvent event) {
        abrirVentana("/inventory_view.fxml", "Gestión de Inventario", false, this::cargarMetricas);
    }

    @FXML
    void abrirCompras(ActionEvent event) {
        abrirVentana("/compras_view.fxml", "Compras de Mercadería", false, () -> {
            cargarMetricas();
            if (tabBalances.isSelected()) balancesController.cargar();
        });
    }

    @FXML
    void abrirGastos(ActionEvent event) {
        abrirVentana("/gastos_view.fxml", "Gastos Operativos", false, () -> {
            cargarMetricas();
            if (tabBalances.isSelected()) balancesController.cargar();
        });
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
        dlg.setHeaderText("Sistema de Gestión PyME  —  v1.2.0");
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
            stage.getScene().setRoot(root);
            stage.setTitle("Sistema FedeiaTech - Pyme v1.2.0");
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
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
