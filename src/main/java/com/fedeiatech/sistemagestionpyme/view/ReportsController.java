package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.service.ExportService;
import com.fedeiatech.sistemagestionpyme.service.LicenseService;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import com.fedeiatech.sistemagestionpyme.service.TicketService;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ReportsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ReportsController.class.getName());

    @FXML private AnchorPane rootPane;
    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, Integer> colId;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, Void> colAccion;
    @FXML private Button btnExportar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rootPane.setStyle(ThemeService.getInstance().getBgStyle());

        if (!LicenseService.permiteReportes()) {
            mostrarAlerta("Acceso Denegado", "No tienes licencia para ver este módulo.");
            return;
        }

        configurarTabla();
        cargarDatos();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        Callback<TableColumn<Venta, Void>, TableCell<Venta, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Venta, Void> call(final TableColumn<Venta, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("🖨️ Ver Ticket");

                    {
                        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                        btn.setOnAction((ActionEvent event) -> {
                            Venta ventaSeleccionada = getTableView().getItems().get(getIndex());
                            if (ventaSeleccionada != null) {
                                reimprimirTicket(ventaSeleccionada.getId());
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btn);
                    }
                };
            }
        };
        colAccion.setCellFactory(cellFactory);
    }

    private void cargarDatos() {
        VentaDAO dao = new VentaDAO();
        try {
            List<Venta> historial = dao.listarVentasHistoricas();
            tablaVentas.setItems(FXCollections.observableArrayList(historial));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el historial de ventas", e);
            mostrarAlerta("Error BD", "No se pudo cargar el historial.");
        }
    }

    @FXML
    void exportarExcel(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Exportar a Excel");
        dialog.setHeaderText("Seleccioná el período y tipo de reporte");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker pickerDesde = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker pickerHasta = new DatePicker(LocalDate.now());
        ChoiceBox<String> tipoReporte = new ChoiceBox<>();
        tipoReporte.getItems().addAll("Resumen diario", "Por producto", "Detalle completo");
        tipoReporte.setValue("Resumen diario");
        tipoReporte.setPrefWidth(200);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Desde:"), 0, 0);
        grid.add(pickerDesde, 1, 0);
        grid.add(new Label("Hasta:"), 0, 1);
        grid.add(pickerHasta, 1, 1);
        grid.add(new Label("Tipo:"), 0, 2);
        grid.add(tipoReporte, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String desde = pickerDesde.getValue().toString();
        String hasta = pickerHasta.getValue().toString();
        String tipo = tipoReporte.getValue();

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("ventas_" + tipo.replace(" ", "_").toLowerCase() + "_" + desde + ".xlsx");
        String docPath = System.getProperty("user.home") + "/Documents";
        File docDir = new File(docPath);
        if (docDir.exists()) fc.setInitialDirectory(docDir);
        File archivo = fc.showSaveDialog(btnExportar.getScene().getWindow());
        if (archivo == null) return;

        try {
            VentaDAO dao = new VentaDAO();
            ExportService exportService = new ExportService();
            List<String[]> datos;
            File generado;

            switch (tipo) {
                case "Por producto" -> {
                    datos = dao.obtenerVentasPorProducto(desde, hasta);
                    generado = exportService.exportarPorProducto(datos, archivo);
                }
                case "Detalle completo" -> {
                    datos = dao.obtenerDetalleCompleto(desde, hasta);
                    generado = exportService.exportarDetalleCompleto(datos, archivo);
                }
                default -> {
                    datos = dao.obtenerResumenDiario(desde, hasta);
                    generado = exportService.exportarResumenDiario(datos, archivo);
                }
            }

            exportService.abrirArchivo(generado);
            mostrarInfo("Exportación exitosa",
                    datos.size() + " filas exportadas a:\n" + generado.getName());

        } catch (Exception e) {
            mostrarAlerta("Error al exportar", e.getMessage());
        }
    }

    private void reimprimirTicket(int idVenta) {
        try {
            VentaDAO dao = new VentaDAO();
            Venta ventaCompleta = dao.obtenerVentaCompleta(idVenta);
            if (ventaCompleta == null) {
                mostrarAlerta("Error", "No se encontró la venta ID " + idVenta);
                return;
            }
            TicketService ts = new TicketService();
            File ticket = ts.generarTicketPDF(ventaCompleta);
            if (ticket != null && ticket.exists()) {
                ts.abrirArchivo(ticket);
            } else {
                mostrarAlerta("Error PDF", "El archivo PDF no se pudo generar.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al reimprimir ticket", e);
            mostrarAlerta("Error Crítico", "Fallo al reimprimir: " + e.getMessage());
        }
    }

    @FXML
    void abrirEstadisticas(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/stats_view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Estadísticas Avanzadas");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo abrir estadísticas: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void mostrarInfo(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
