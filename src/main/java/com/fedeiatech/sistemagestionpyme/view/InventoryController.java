package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import com.fedeiatech.sistemagestionpyme.service.ExportService;
import com.fedeiatech.sistemagestionpyme.service.ImportService;
import com.fedeiatech.sistemagestionpyme.service.ImportService.ImportResult;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

public class InventoryController implements Initializable {

    @FXML private TableView<ItemVenta> tablaItems;
    @FXML private TableColumn<ItemVenta, Integer> colId;
    @FXML private TableColumn<ItemVenta, String> colCodigo;
    @FXML private TableColumn<ItemVenta, String> colNombre;
    @FXML private TableColumn<ItemVenta, Double> colPrecio;
    @FXML private TableColumn<ItemVenta, Double> colStock;

    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private CheckBox chkServicio;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Label lblFormTitulo;

    private ItemDAO itemDAO;
    private ObservableList<ItemVenta> listaItems;
    private ItemVenta itemEnEdicion = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        itemDAO = new ItemDAO();

        cmbUnidad.getItems().addAll("u", "kg", "g", "lt");
        cmbUnidad.setValue("u");

        configurarColumnas();
        cargarDatos();

        tablaItems.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                entrarModoEdicion(seleccionado);
            }
        });

        chkServicio.selectedProperty().addListener((obs, anterior, seleccionado) -> {
            txtStock.setDisable(seleccionado);
            cmbUnidad.setDisable(seleccionado);
            if (seleccionado) {
                txtStock.clear();
                cmbUnidad.setValue("u");
            }
        });
    }

    private void entrarModoEdicion(ItemVenta item) {
        itemEnEdicion = item;
        txtCodigo.setText(item.getCodigo());
        txtNombre.setText(item.getNombre());
        txtPrecio.setText(String.valueOf(item.getPrecioVenta()));
        chkServicio.setSelected(item.isEsServicio());

        if (item.isEsServicio()) {
            txtStock.clear();
            txtStock.setDisable(true);
            cmbUnidad.setValue("u");
            cmbUnidad.setDisable(true);
        } else {
            txtStock.setText(String.valueOf(item.getStock()));
            txtStock.setDisable(false);
            cmbUnidad.setValue(item.getUnidad());
            cmbUnidad.setDisable(false);
        }

        lblFormTitulo.setText("Editando: " + item.getNombre());
        btnGuardar.setText("ACTUALIZAR");
        btnGuardar.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCancelar.setVisible(true);
        btnCancelar.setManaged(true);
    }

    @FXML
    void cancelarEdicion(ActionEvent event) {
        itemEnEdicion = null;
        limpiarFormulario();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        colStock.setCellFactory(column -> new TableCell<ItemVenta, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                ItemVenta rowData = getTableRow().getItem();
                if (rowData == null) return;

                if (rowData.isEsServicio()) {
                    setText("Servicio");
                    setTextFill(Color.BLUE);
                    setStyle("-fx-font-weight: bold; -fx-alignment: CENTER;");
                    return;
                }

                if (item == null) {
                    setText("0 " + rowData.getUnidad());
                    setTextFill(Color.ORANGE);
                    setStyle("-fx-alignment: CENTER_RIGHT;");
                    return;
                }

                setText(item % 1 == 0
                        ? (int) item.doubleValue() + " " + rowData.getUnidad()
                        : item + " " + rowData.getUnidad());
                setStyle("-fx-alignment: CENTER_RIGHT;");

                if (item < 0) {
                    setTextFill(Color.RED);
                    setStyle("-fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                } else if (item == 0) {
                    setTextFill(Color.ORANGE);
                } else {
                    setTextFill(Color.BLACK);
                }
            }
        });
    }

    private void cargarDatos() {
        try {
            listaItems = FXCollections.observableArrayList(itemDAO.listarTodos());
            tablaItems.setItems(listaItems);
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error DB", "No se pudo cargar la lista: " + e.getMessage());
        }
    }

    @FXML
    void guardarItem(ActionEvent event) {
        try {
            if (txtCodigo.getText().isEmpty() || txtNombre.getText().isEmpty() || txtPrecio.getText().isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos", "Por favor llena Código, Nombre y Precio.");
                return;
            }

            ItemVenta item = (itemEnEdicion != null) ? itemEnEdicion : new ItemVenta();
            item.setCodigo(txtCodigo.getText());
            item.setNombre(txtNombre.getText());
            item.setDescripcion(item.getDescripcion() != null ? item.getDescripcion() : "");
            item.setPrecioCosto(item.getPrecioCosto());
            item.setPrecioVenta(Double.parseDouble(txtPrecio.getText()));

            if (chkServicio.isSelected()) {
                item.setEsServicio(true);
                item.setStock(-1);
                item.setUnidad("u");
            } else {
                item.setEsServicio(false);
                String stockStr = txtStock.getText().isEmpty() ? "0" : txtStock.getText();
                item.setStock(Double.parseDouble(stockStr));
                item.setUnidad(cmbUnidad.getValue() != null ? cmbUnidad.getValue() : "u");
            }

            if (itemEnEdicion != null) {
                itemDAO.actualizar(item);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto actualizado correctamente.");
            } else {
                itemDAO.guardar(item);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto guardado correctamente.");
            }

            cargarDatos();
            limpiarFormulario();

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Formato", "El Precio y Stock deben ser números válidos.");
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error Base de Datos", "No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    void eliminarItem(ActionEvent event) {
        ItemVenta itemSeleccionado = tablaItems.getSelectionModel().getSelectedItem();
        if (itemSeleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Selecciona un producto de la lista para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Estás seguro?");
        confirmacion.setContentText("Vas a eliminar: " + itemSeleccionado.getNombre());

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                itemDAO.eliminar(itemSeleccionado.getId());
                cargarDatos();
                mostrarAlerta(Alert.AlertType.INFORMATION, "Eliminado", "Producto eliminado.");
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo eliminar: " + e.getMessage());
            }
        }
    }

    @FXML
    void descargarPlantilla(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar plantilla de inventario");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("plantilla_inventario.xlsx");
        File docDir = new File(System.getProperty("user.home") + "/Documents");
        if (docDir.exists()) fc.setInitialDirectory(docDir);
        File destino = fc.showSaveDialog(btnGuardar.getScene().getWindow());
        if (destino == null) return;

        try {
            File generado = new ExportService().generarPlantillaInventario(destino);
            new ExportService().abrirArchivo(generado);
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo generar la plantilla: " + e.getMessage());
        }
    }

    @FXML
    void importarExcel(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File docDir = new File(System.getProperty("user.home") + "/Documents");
        if (docDir.exists()) fc.setInitialDirectory(docDir);
        File archivo = fc.showOpenDialog(btnGuardar.getScene().getWindow());
        if (archivo == null) return;

        ImportResult result;
        try {
            result = new ImportService().importarDesdeExcel(archivo);
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al leer archivo", e.getMessage());
            return;
        }

        if (result.validos.isEmpty() && result.errores.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Archivo vacío", "El archivo no contiene datos.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Importar inventario");
        confirm.setHeaderText(result.validos.size() + " producto(s) válido(s) para importar" +
                (result.errores.isEmpty() ? "" : "\n⚠ " + result.errores.size() + " fila(s) con errores — no se importarán"));

        if (!result.errores.isEmpty()) {
            TextArea ta = new TextArea(String.join("\n", result.errores));
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefHeight(150);
            confirm.getDialogPane().setExpandableContent(ta);
            confirm.getDialogPane().setExpanded(result.validos.isEmpty());
        }

        if (result.validos.isEmpty()) {
            confirm.getButtonTypes().setAll(ButtonType.OK);
            confirm.setContentText("No hay productos válidos para importar.");
            confirm.showAndWait();
            return;
        }

        confirm.setContentText("¿Continuar con la importación?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        List<ItemVenta> nuevos = new ArrayList<>();
        List<ItemVenta> duplicados = new ArrayList<>();
        try {
            for (ItemVenta item : result.validos) {
                ItemVenta existente = itemDAO.buscarPorCodigo(item.getCodigo());
                if (existente != null) {
                    item.setId(existente.getId());
                    duplicados.add(item);
                } else {
                    nuevos.add(item);
                }
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error BD", e.getMessage());
            return;
        }

        boolean actualizarDuplicados = false;
        if (!duplicados.isEmpty()) {
            ButtonType btnActualizar = new ButtonType("Actualizar");
            ButtonType btnSaltar = new ButtonType("Saltar");
            ButtonType btnCancelarDup = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert dupAlert = new Alert(Alert.AlertType.CONFIRMATION);
            dupAlert.setTitle("Productos duplicados");
            dupAlert.setHeaderText(duplicados.size() + " producto(s) ya existen con el mismo código.");
            dupAlert.setContentText("¿Qué deseas hacer con ellos?");
            dupAlert.getButtonTypes().setAll(btnActualizar, btnSaltar, btnCancelarDup);

            Optional<ButtonType> dupRes = dupAlert.showAndWait();
            if (dupRes.isEmpty() || dupRes.get() == btnCancelarDup) return;
            actualizarDuplicados = dupRes.get() == btnActualizar;
        }

        int guardados = 0;
        int saltados = 0;
        List<String> erroresBD = new ArrayList<>();

        for (ItemVenta item : nuevos) {
            try {
                itemDAO.guardar(item);
                guardados++;
            } catch (SQLException e) {
                erroresBD.add(item.getCodigo() + ": " + e.getMessage());
            }
        }

        if (actualizarDuplicados) {
            for (ItemVenta item : duplicados) {
                try {
                    itemDAO.actualizar(item);
                    guardados++;
                } catch (SQLException e) {
                    erroresBD.add(item.getCodigo() + ": " + e.getMessage());
                }
            }
        } else {
            saltados = duplicados.size();
        }

        cargarDatos();

        String resumen = guardados + " producto(s) importado(s)";
        if (saltados > 0) resumen += "\n" + saltados + " saltado(s) por duplicado";
        if (!erroresBD.isEmpty()) resumen += "\n" + erroresBD.size() + " error(es) de base de datos";
        mostrarAlerta(Alert.AlertType.INFORMATION, "Importación completada", resumen);
    }

    @FXML
    void mostrarInfoImport(ActionEvent event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Formato de importación Excel");
        info.setHeaderText("Columnas requeridas en el archivo");
        info.setContentText(
            "A  Código        — Identificador único (requerido)\n" +
            "B  Nombre        — Nombre del producto (requerido)\n" +
            "C  Descripción   — Texto libre (opcional)\n" +
            "D  Precio Costo  — Número ≥ 0 (opcional, default 0)\n" +
            "E  Precio Venta  — Número ≥ 0 (requerido)\n" +
            "F  Stock         — Número ≥ 0 (ignorado si es servicio)\n" +
            "G  Unidad        — u / kg / g / lt  (default: u)\n" +
            "H  Es Servicio   — SI o NO\n\n" +
            "Tip: usá 'Plantilla' para descargar el formato correcto."
        );
        info.showAndWait();
    }

    private void limpiarFormulario() {
        itemEnEdicion = null;
        tablaItems.getSelectionModel().clearSelection();
        txtCodigo.clear();
        txtNombre.clear();
        txtPrecio.clear();
        txtStock.clear();
        txtStock.setDisable(false);
        cmbUnidad.setValue("u");
        cmbUnidad.setDisable(false);
        chkServicio.setSelected(false);
        lblFormTitulo.setText("Nuevo Producto / Servicio:");
        btnGuardar.setText("AGREGAR");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
