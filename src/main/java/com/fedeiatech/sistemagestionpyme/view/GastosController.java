package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.GastoDAO;
import com.fedeiatech.sistemagestionpyme.model.Gasto;
import com.fedeiatech.sistemagestionpyme.service.ExportService;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import com.fedeiatech.sistemagestionpyme.view.util.AlertUtil;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class GastosController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private VBox formVBox;
    @FXML private TextField txtConcepto;
    @FXML private TextField txtMonto;
    @FXML private TextField txtCategoria;
    @FXML private DatePicker dpFecha;
    @FXML private Button btnGuardar;
    @FXML private Button btnExportar;

    @FXML private TableView<Gasto> tablaGastos;
    @FXML private TableColumn<Gasto, String> colFecha;
    @FXML private TableColumn<Gasto, String> colConcepto;
    @FXML private TableColumn<Gasto, String> colCategoria;
    @FXML private TableColumn<Gasto, Double> colMonto;
    @FXML private TableColumn<Gasto, Void> colAccion;

    private final GastoDAO gastoDAO = new GastoDAO();
    private final ObservableList<Gasto> historico = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (rootPane != null) rootPane.setStyle(ThemeService.getInstance().getBgStyle());
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, sceneAnterior, sceneNueva) -> {
                if (sceneNueva != null) {
                    sceneNueva.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ESCAPE) {
                            ((Stage) sceneNueva.getWindow()).close();
                        }
                    });
                }
            });
        }

        dpFecha.setValue(LocalDate.now());

        configurarTabla();
        cargarHistorico();

        if (!SessionService.getInstance().esAdmin()) {
            formVBox.setVisible(false);
            formVBox.setManaged(false);
        }
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));

        colAccion.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final Button btnEliminar = new Button("Eliminar");
            {
                btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                btnEliminar.setOnAction(e -> eliminarGasto(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnEliminar);
            }
        });
        colAccion.setVisible(SessionService.getInstance().esAdmin());

        tablaGastos.setItems(historico);
    }

    private void cargarHistorico() {
        try {
            historico.setAll(gastoDAO.listarTodos());
        } catch (SQLException e) {
            AlertUtil.mostrarError("Error DB", "No se pudo cargar el histórico de gastos: " + e.getMessage());
        }
    }

    private void eliminarGasto(Gasto gasto) {
        if (gasto == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar gasto");
        confirm.setHeaderText("Esta acción es irreversible.");
        confirm.setContentText("¿Confirmás eliminar el gasto \"" + gasto.getConcepto() + "\" por ARS "
                + String.format("%.2f", gasto.getMonto()) + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            gastoDAO.eliminar(gasto.getId());
            historico.remove(gasto);
        } catch (SecurityException e) {
            AlertUtil.mostrarAdvertencia("Permiso denegado", e.getMessage());
        } catch (SQLException e) {
            AlertUtil.mostrarError("Error DB", "No se pudo eliminar el gasto: " + e.getMessage());
        }
    }

    @FXML
    void guardar(ActionEvent event) {
        String concepto = txtConcepto.getText() != null ? txtConcepto.getText().trim() : "";
        if (concepto.isEmpty()) {
            AlertUtil.mostrarAdvertencia("Falta el concepto", "Ingresá el concepto del gasto.");
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(txtMonto.getText().trim().replace(",", "."));
        } catch (Exception e) {
            AlertUtil.mostrarAdvertencia("Error de formato", "El monto debe ser un número válido.");
            return;
        }
        if (monto <= 0) {
            AlertUtil.mostrarAdvertencia("Monto inválido", "El monto debe ser mayor a cero.");
            return;
        }
        if (dpFecha.getValue() == null) {
            AlertUtil.mostrarAdvertencia("Falta la fecha", "Seleccioná la fecha del gasto.");
            return;
        }

        Gasto gasto = new Gasto();
        gasto.setConcepto(concepto);
        gasto.setMonto(monto);
        String categoria = txtCategoria.getText() != null ? txtCategoria.getText().trim() : "";
        gasto.setCategoria(categoria.isEmpty() ? null : categoria);
        gasto.setFecha(dpFecha.getValue().toString());
        gasto.setUsuario(SessionService.getInstance().getUsuarioActivo() != null
                ? SessionService.getInstance().getUsuarioActivo().getNombre() : null);

        try {
            gastoDAO.registrar(gasto);
            AlertUtil.mostrarInfo("Gasto registrado", "El gasto se registró correctamente.");
            limpiarFormulario();
            cargarHistorico();
        } catch (SecurityException e) {
            AlertUtil.mostrarAdvertencia("Permiso denegado", e.getMessage());
        } catch (SQLException e) {
            AlertUtil.mostrarError("Error DB", "No se pudo registrar el gasto: " + e.getMessage());
        }
    }

    private void limpiarFormulario() {
        txtConcepto.clear();
        txtMonto.clear();
        txtCategoria.clear();
        dpFecha.setValue(LocalDate.now());
    }

    @FXML
    void exportarExcel(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Exportar gastos a Excel");
        dialog.setHeaderText("Seleccioná el período a exportar");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker pickerDesde = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker pickerHasta = new DatePicker(LocalDate.now());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Desde:"), 0, 0);
        grid.add(pickerDesde, 1, 0);
        grid.add(new Label("Hasta:"), 0, 1);
        grid.add(pickerHasta, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String desde = pickerDesde.getValue().toString();
        String hasta = pickerHasta.getValue().toString();

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar gastos Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("gastos_" + desde + "_" + hasta + ".xlsx");
        String docPath = System.getProperty("user.home") + "/Documents";
        File docDir = new File(docPath);
        if (docDir.exists()) fc.setInitialDirectory(docDir);
        File archivo = fc.showSaveDialog(btnExportar.getScene().getWindow());
        if (archivo == null) return;

        try {
            List<Gasto> gastos = gastoDAO.listarEntre(desde, hasta);
            List<String[]> datos = gastos.stream()
                    .map(g -> new String[]{
                        g.getFecha(),
                        g.getConcepto(),
                        g.getCategoria() != null ? g.getCategoria() : "",
                        String.valueOf(g.getMonto())
                    })
                    .toList();

            ExportService exportService = new ExportService();
            File generado = exportService.exportarGastos(datos, archivo);
            exportService.abrirArchivo(generado);
            AlertUtil.mostrarInfo("Exportación exitosa", datos.size() + " filas exportadas a:\n" + generado.getName());
        } catch (Exception e) {
            AlertUtil.mostrarAdvertencia("Error al exportar", e.getMessage());
        }
    }

    @FXML
    void cancelar(ActionEvent event) {
        ((Stage) tablaGastos.getScene().getWindow()).close();
    }
}
