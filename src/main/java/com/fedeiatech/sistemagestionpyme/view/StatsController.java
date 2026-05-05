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

public class StatsController implements Initializable {

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
        cargarDatos();
    }

    private void configurarTablaBasket() {
        colProdA.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colProdB.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colFrecuencia.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
    }

    private void cargarDatos() {
        VentaDAO dao = new VentaDAO();
        try {
            cargarMarketBasket(dao);
            cargarHorarios(dao);
            cargarHeatmap(dao);
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            lblMejorHorario.setText(String.format("Mejor horario: %02d:00 — %02d:59  ($ %.2f acumulado)", mejorHora, mejorHora, maxTotal));
        } else {
            lblMejorHorario.setText("Sin datos de horarios aún.");
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
