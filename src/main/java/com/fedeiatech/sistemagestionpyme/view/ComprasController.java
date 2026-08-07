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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ComprasController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private VBox formVBox;
    @FXML private TextField txtBuscarProducto;
    @FXML private Label lblProductoElegido;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtCostoUnitario;
    @FXML private Label lblCostoTotal;
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (rootPane != null) rootPane.setStyle(ThemeService.getInstance().getBgStyle());

        dpFecha.setValue(LocalDate.now());

        txtCantidad.textProperty().addListener((obs, viejo, nuevo) -> recalcularTotal());
        txtCostoUnitario.textProperty().addListener((obs, viejo, nuevo) -> recalcularTotal());

        configurarTabla();
        cargarHistorico();

        if (!SessionService.getInstance().esAdmin()) {
            formVBox.setVisible(false);
            formVBox.setManaged(false);
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
        txtBuscarProducto.clear();
    }

    private String formatearCantidad(double valor) {
        return valor % 1 == 0 ? String.valueOf((int) valor) : String.valueOf(valor);
    }

    private void recalcularTotal() {
        double cantidad = parsearODefault(txtCantidad.getText());
        double costoUnitario = parsearODefault(txtCostoUnitario.getText());
        lblCostoTotal.setText(String.format("ARS %.2f", cantidad * costoUnitario));
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
        txtBuscarProducto.clear();
        txtCantidad.clear();
        txtCostoUnitario.clear();
        txtProveedor.clear();
        dpFecha.setValue(LocalDate.now());
        recalcularTotal();
    }

    @FXML
    void cancelar(ActionEvent event) {
        ((Stage) txtBuscarProducto.getScene().getWindow()).close();
    }
}
