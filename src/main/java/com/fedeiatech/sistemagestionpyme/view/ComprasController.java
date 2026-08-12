package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.CompraDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.model.Compra;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import com.fedeiatech.sistemagestionpyme.view.util.AlertUtil;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class ComprasController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private VBox formVBox;
    @FXML private TextField txtBuscarProducto;
    @FXML private Label lblProductoElegido;
    @FXML private Label lblCantidadUnidad;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtCostoUnitario;
    @FXML private TextField txtCostoTotal;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtProveedor;
    @FXML private Button btnGuardar;

    @FXML private TableView<Compra> tablaCompras;
    @FXML private TableColumn<Compra, String> colFecha;
    @FXML private TableColumn<Compra, String> colProducto;
    @FXML private TableColumn<Compra, Double> colCantidad;
    @FXML private TableColumn<Compra, Double> colCostoUnitario;
    @FXML private TableColumn<Compra, Double> colCostoTotal;
    @FXML private TableColumn<Compra, String> colProveedor;

    private final ItemDAO itemDAO = new ItemDAO();
    private final CompraDAO compraDAO = new CompraDAO();
    private final ObservableList<Compra> historico = FXCollections.observableArrayList();

    private ItemVenta productoElegido;
    private Popup popupSugerencias;
    private ListView<ItemVenta> listSugerencias;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (rootPane != null) rootPane.setStyle(ThemeService.getInstance().getBgStyle());
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, sceneAnterior, sceneNueva) -> {
                if (sceneNueva != null) {
                    sceneNueva.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ESCAPE) {
                            if (popupSugerencias != null && popupSugerencias.isShowing()) {
                                popupSugerencias.hide();
                            } else {
                                ((Stage) sceneNueva.getWindow()).close();
                            }
                        }
                    });
                }
            });
        }

        dpFecha.setValue(LocalDate.now());

        txtCantidad.textProperty().addListener((obs, viejo, nuevo) -> recalcularTotalDesdeUnitario());
        txtCostoUnitario.textProperty().addListener((obs, viejo, nuevo) -> recalcularTotalDesdeUnitario());
        txtCostoTotal.textProperty().addListener((obs, viejo, nuevo) -> recalcularUnitarioDesdeTotal());

        configurarAutocompletado();
        configurarTabla();
        cargarHistorico();

        if (!SessionService.getInstance().esAdmin()) {
            formVBox.setVisible(false);
            formVBox.setManaged(false);
        }
    }

    private void configurarAutocompletado() {
        listSugerencias = new ListView<>();
        listSugerencias.setPrefWidth(320);
        listSugerencias.setMaxHeight(220);
        listSugerencias.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ItemVenta item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });

        popupSugerencias = new Popup();
        popupSugerencias.setAutoHide(true);
        popupSugerencias.setHideOnEscape(true);
        popupSugerencias.getContent().add(listSugerencias);

        txtBuscarProducto.textProperty().addListener((obs, valorViejo, valorNuevo) -> actualizarSugerencias(valorNuevo));

        listSugerencias.setOnMouseClicked(event -> {
            ItemVenta seleccionado = listSugerencias.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                seleccionarProducto(seleccionado);
                popupSugerencias.hide();
            }
        });

        listSugerencias.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                ItemVenta seleccionado = listSugerencias.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    seleccionarProducto(seleccionado);
                    popupSugerencias.hide();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                popupSugerencias.hide();
                txtBuscarProducto.requestFocus();
            }
        });

        txtBuscarProducto.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN && popupSugerencias.isShowing()) {
                listSugerencias.requestFocus();
                listSugerencias.getSelectionModel().selectFirst();
                event.consume();
            }
        });
    }

    private void actualizarSugerencias(String termino) {
        String t = termino == null ? "" : termino.trim();
        if (t.length() < 2) {
            popupSugerencias.hide();
            return;
        }

        try {
            List<ItemVenta> sugerencias = itemDAO.buscarPorFiltro(t).stream()
                    .filter(i -> !i.isEsServicio() && !i.isEsCombo())
                    .toList();

            if (sugerencias.isEmpty()) {
                popupSugerencias.hide();
                return;
            }

            listSugerencias.getItems().setAll(
                sugerencias.size() > 8 ? sugerencias.subList(0, 8) : sugerencias);
            listSugerencias.getSelectionModel().clearSelection();

            if (!popupSugerencias.isShowing()) {
                var bounds = txtBuscarProducto.localToScreen(txtBuscarProducto.getBoundsInLocal());
                popupSugerencias.show(txtBuscarProducto, bounds.getMinX(), bounds.getMaxY());
            }
        } catch (SQLException e) {
            popupSugerencias.hide();
        }
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreItem"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCostoUnitario.setCellValueFactory(new PropertyValueFactory<>("costoUnitario"));
        colCostoTotal.setCellValueFactory(new PropertyValueFactory<>("costoTotal"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        tablaCompras.setItems(historico);
    }

    private void cargarHistorico() {
        try {
            historico.setAll(compraDAO.listarHistorico(50));
        } catch (SQLException e) {
            AlertUtil.mostrarError("Error DB", "No se pudo cargar el histórico de compras: " + e.getMessage());
        }
    }

    @FXML
    void buscarProducto(ActionEvent event) {
        String termino = txtBuscarProducto.getText().trim();
        if (termino.isEmpty()) return;

        try {
            List<ItemVenta> resultados = itemDAO.buscarPorFiltro(termino).stream()
                    .filter(i -> !i.isEsServicio() && !i.isEsCombo())
                    .toList();

            if (resultados.isEmpty()) {
                AlertUtil.mostrarAdvertencia("No encontrado",
                        "No se encontró ningún producto físico (no combo, no servicio) con ese criterio.");
                return;
            }

            if (resultados.size() == 1) {
                seleccionarProducto(resultados.get(0));
            } else {
                ChoiceDialog<ItemVenta> dialog = new ChoiceDialog<>(resultados.get(0), resultados);
                dialog.setTitle("Seleccionar Producto");
                dialog.setHeaderText("Múltiples coincidencias encontradas");
                dialog.setContentText("Elegí el producto comprado:");
                dialog.showAndWait().ifPresent(this::seleccionarProducto);
            }
        } catch (SQLException e) {
            AlertUtil.mostrarError("Error DB", e.getMessage());
        }
    }

    private void seleccionarProducto(ItemVenta item) {
        if (item.isEsCombo()) {
            AlertUtil.mostrarAdvertencia("No disponible",
                    "No se pueden comprar combos directamente — comprá sus componentes individualmente.");
            return;
        }
        if (item.isEsServicio()) {
            AlertUtil.mostrarAdvertencia("No disponible", "Los servicios no tienen stock físico para comprar.");
            return;
        }
        productoElegido = item;
        lblProductoElegido.setText(item.getCodigo() + " · " + item.getNombre()
                + " · stock actual " + formatearCantidad(item.getStock()) + " " + item.getUnidad()
                + " · costo actual $" + String.format("%.2f", item.getPrecioCosto()));
        lblProductoElegido.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: black;");
        lblCantidadUnidad.setText("- " + item.getUnidad());
        txtBuscarProducto.clear();
    }

    private String formatearCantidad(double valor) {
        return valor % 1 == 0 ? String.valueOf((int) valor) : String.valueOf(valor);
    }

    private boolean actualizandoCostos = false;

    private void recalcularTotalDesdeUnitario() {
        if (actualizandoCostos) return;
        actualizandoCostos = true;
        try {
            double cantidad = parsearODefault(txtCantidad.getText());
            double costoUnitario = parsearODefault(txtCostoUnitario.getText());
            txtCostoTotal.setText(String.format("%.2f", cantidad * costoUnitario));
        } finally {
            actualizandoCostos = false;
        }
    }

    private void recalcularUnitarioDesdeTotal() {
        if (actualizandoCostos) return;
        actualizandoCostos = true;
        try {
            double cantidad = parsearODefault(txtCantidad.getText());
            double costoTotal = parsearODefault(txtCostoTotal.getText());
            if (cantidad > 0) {
                txtCostoUnitario.setText(String.format("%.2f", costoTotal / cantidad));
            }
        } finally {
            actualizandoCostos = false;
        }
    }

    private double parsearODefault(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    @FXML
    void guardar(ActionEvent event) {
        if (productoElegido == null) {
            AlertUtil.mostrarAdvertencia("Falta el producto", "Buscá y seleccioná el producto comprado antes de guardar.");
            return;
        }

        double cantidad;
        double costoUnitario;
        try {
            cantidad = Double.parseDouble(txtCantidad.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            AlertUtil.mostrarAdvertencia("Error de formato", "La cantidad debe ser un número válido.");
            return;
        }
        try {
            costoUnitario = Double.parseDouble(txtCostoUnitario.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            AlertUtil.mostrarAdvertencia("Error de formato", "El costo unitario debe ser un número válido.");
            return;
        }

        if (cantidad <= 0) {
            AlertUtil.mostrarAdvertencia("Cantidad inválida", "La cantidad debe ser mayor a cero.");
            return;
        }
        if (costoUnitario < 0) {
            AlertUtil.mostrarAdvertencia("Costo inválido", "El costo unitario no puede ser negativo.");
            return;
        }
        if (dpFecha.getValue() == null) {
            AlertUtil.mostrarAdvertencia("Falta la fecha", "Seleccioná la fecha de la compra.");
            return;
        }

        Compra compra = new Compra();
        compra.setIdItem(productoElegido.getId());
        compra.setCantidad(cantidad);
        compra.setCostoUnitario(costoUnitario);
        compra.calcularTotal();
        compra.setFecha(dpFecha.getValue().toString());
        String proveedor = txtProveedor.getText() != null ? txtProveedor.getText().trim() : "";
        compra.setProveedor(proveedor.isEmpty() ? null : proveedor);

        try {
            compraDAO.registrarCompra(compra);
            AlertUtil.mostrarInfo("Compra registrada", "La compra se registró correctamente y el stock fue actualizado.");
            limpiarFormulario();
            cargarHistorico();
        } catch (SQLException e) {
            AlertUtil.mostrarError("Error DB", "No se pudo registrar la compra: " + e.getMessage());
        }
    }

    private void limpiarFormulario() {
        productoElegido = null;
        lblProductoElegido.setText("Ningún producto seleccionado");
        lblProductoElegido.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");
        lblCantidadUnidad.setText("");
        txtBuscarProducto.clear();
        txtCantidad.clear();
        txtCostoUnitario.clear();
        txtCostoTotal.clear();
        txtProveedor.clear();
        dpFecha.setValue(LocalDate.now());
    }

    @FXML
    void cancelar(ActionEvent event) {
        ((Stage) txtBuscarProducto.getScene().getWindow()).close();
    }
}
