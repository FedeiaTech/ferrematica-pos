package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.service.LicenseService;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(StatsController.class.getName());

    @FXML private AnchorPane rootPane;
    @FXML private Label lblPremiumLock;
    @FXML private TabPane tabPane;
    @FXML private TableView<String[]> tablaBasket;
    @FXML private TableColumn<String[], String> colProdA;
    @FXML private TableColumn<String[], String> colProdB;
    @FXML private TableColumn<String[], String> colFrecuencia;
    @FXML private BarChart<String, Number> chartHoras;
    @FXML private Label lblMejorHorario;
    @FXML private GridPane gridHeatmap;
    @FXML private BarChart<String, Number> chartSemanal;
    @FXML private BarChart<String, Number> chartMensual;
    @FXML private Label lblResumenSemanal;
    @FXML private Label lblResumenMensual;
    @FXML private TableView<String[]> tablaSemanal;
    @FXML private TableColumn<String[], String> colSemana;
    @FXML private TableColumn<String[], String> colCantSemanal;
    @FXML private TableColumn<String[], String> colTotalSemanal;
    @FXML private TableColumn<String[], String> colPromedioSemanal;

    private static final String[] DIAS = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rootPane.setStyle(ThemeService.getInstance().getBgStyle());

        if (!LicenseService.permiteEstadisticas()) {
            lblPremiumLock.setVisible(true);
            lblPremiumLock.setManaged(true);
            tabPane.setDisable(true);
            return;
        }

        configurarTablaBasket();
        configurarTablaSemanal();
        cargarDatos();
    }

    private void configurarTablaBasket() {
        colProdA.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colProdB.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colFrecuencia.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
    }

    private void configurarTablaSemanal() {
        colSemana.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colCantSemanal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colTotalSemanal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            String.format("$ %.2f", Double.parseDouble(data.getValue()[2]))));
        colPromedioSemanal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            String.format("$ %.2f", Double.parseDouble(data.getValue()[3]))));
    }

    private void cargarDatos() {
        VentaDAO dao = new VentaDAO();
        try { cargarMarketBasket(dao); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error al cargar market basket", e); }
        try { cargarHorarios(dao); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error al cargar horarios", e); }
        try { cargarHeatmap(dao); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error al cargar heatmap", e); }
        try { cargarTendencias(dao); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error al cargar tendencias", e); }
    }

    private void cargarMarketBasket(VentaDAO dao) throws SQLException {
        List<String[]> datos = dao.obtenerMarketBasket();
        ObservableList<String[]> lista = FXCollections.observableArrayList(datos);
        tablaBasket.setItems(lista);
        if (datos.isEmpty()) {
            lblPremiumLock.setText("ℹ No hay suficientes datos para mostrar correlaciones (mínimo 2 tickets con pares).");
            lblPremiumLock.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");
            lblPremiumLock.setVisible(true);
            lblPremiumLock.setManaged(true);
        }
    }

    private void cargarHorarios(VentaDAO dao) throws SQLException {
        Map<Integer, Double> datos = dao.obtenerTotalesPorHora();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        double maxTotal = 0;
        int mejorHora = -1;

        for (Map.Entry<Integer, Double> e : datos.entrySet()) {
            String hora = String.format("%02d:00", e.getKey());
            serie.getData().add(new XYChart.Data<>(hora, e.getValue()));
            if (e.getValue() > maxTotal) {
                maxTotal = e.getValue();
                mejorHora = e.getKey();
            }
        }

        chartHoras.getData().clear();
        chartHoras.getData().add(serie);

        if (mejorHora >= 0) {
            lblMejorHorario.setText(String.format(
                "La franja %02d:00 — %02d:59 generó más ingresos en total: $ %.2f (suma histórica de todas las ventas en ese horario).",
                mejorHora, mejorHora, maxTotal));
        } else {
            lblMejorHorario.setText("Sin datos de horarios aún.");
        }
    }

    private void cargarTendencias(VentaDAO dao) throws SQLException {
        // --- Semanal ---
        List<String[]> semanas = dao.obtenerVentasPorSemana();
        XYChart.Series<String, Number> serieSem = new XYChart.Series<>();
        double totalSem = 0; int maxSemCant = 0; String mejorSem = "";
        for (String[] row : semanas) {
            serieSem.getData().add(new XYChart.Data<>(row[0], Double.parseDouble(row[2])));
            totalSem += Double.parseDouble(row[2]);
            int cant = Integer.parseInt(row[1]);
            if (cant > maxSemCant) { maxSemCant = cant; mejorSem = row[0]; }
        }
        chartSemanal.getData().clear();
        chartSemanal.getData().add(serieSem);
        if (!semanas.isEmpty()) {
            lblResumenSemanal.setText(String.format(
                "%d semanas registradas — Total acumulado: $ %.2f — Semana más activa: %s (%d ventas)",
                semanas.size(), totalSem, mejorSem, maxSemCant));
        }
        tablaSemanal.setItems(javafx.collections.FXCollections.observableArrayList(semanas));

        // --- Mensual ---
        List<String[]> meses = dao.obtenerVentasPorMes();
        XYChart.Series<String, Number> serieMes = new XYChart.Series<>();
        double totalMes = 0; String mejorMes = ""; double maxMesTotal = 0;
        for (String[] row : meses) {
            double t = Double.parseDouble(row[2]);
            serieMes.getData().add(new XYChart.Data<>(row[0], t));
            totalMes += t;
            if (t > maxMesTotal) { maxMesTotal = t; mejorMes = row[0]; }
        }
        chartMensual.getData().clear();
        chartMensual.getData().add(serieMes);
        if (!meses.isEmpty()) {
            lblResumenMensual.setText(String.format(
                "%d meses registrados — Total acumulado: $ %.2f — Mejor mes: %s ($ %.2f)",
                meses.size(), totalMes, mejorMes, maxMesTotal));
        }
    }

    private void cargarHeatmap(VentaDAO dao) throws SQLException {
        Map<String, Double> datos = dao.obtenerHeatmapDiaHora();
        gridHeatmap.getChildren().clear();

        double maxVal = datos.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);

        // Encabezado de horas
        for (int hora = 0; hora < 24; hora++) {
            Label lbl = new Label(String.format("%02d", hora));
            lbl.setStyle("-fx-font-size: 9; -fx-text-fill: #7f8c8d; -fx-min-width: 28; -fx-alignment: CENTER;");
            gridHeatmap.add(lbl, hora + 1, 0);
        }

        // Filas de días
        for (int dia = 0; dia < 7; dia++) {
            Label lblDia = new Label(DIAS[dia]);
            lblDia.setStyle("-fx-font-size: 9; -fx-min-width: 28; -fx-alignment: CENTER_RIGHT;");
            gridHeatmap.add(lblDia, 0, dia + 1);

            for (int hora = 0; hora < 24; hora++) {
                double val = datos.getOrDefault(dia + "-" + hora, 0.0);
                double intensidad = maxVal > 0 ? val / maxVal : 0;
                int r = (int)(255 * (1 - intensidad * 0.7));
                int g = (int)(255 * (1 - intensidad * 0.5));
                int b = (int)(255 * (1 - intensidad));
                String color = String.format("rgb(%d,%d,%d)", r, g, b);

                Label celda = new Label(val > 0 ? String.valueOf((int) val) : "");
                celda.setStyle(String.format(
                    "-fx-background-color: %s; -fx-min-width: 28; -fx-min-height: 20; " +
                    "-fx-alignment: CENTER; -fx-font-size: 8; -fx-text-fill: %s;",
                    color, intensidad > 0.5 ? "white" : "#555"
                ));
                gridHeatmap.add(celda, hora + 1, dia + 1);
            }
        }
    }
}
