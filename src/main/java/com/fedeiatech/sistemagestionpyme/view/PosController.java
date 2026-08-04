package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ComboDAO;
import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Combo;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.service.TicketService;
import com.fedeiatech.sistemagestionpyme.view.util.AlertUtil;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class PosController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private TextField txtBuscador;
    @FXML private Label lblTotal;
    @FXML private Button btnCobrar;
    @FXML private Button btnEliminar;
    @FXML private Button btnVaciar;

    @FXML private TableView<DetalleVenta> tablaDetalles;
    @FXML private TableColumn<DetalleVenta, String> colCodigo;
    @FXML private TableColumn<DetalleVenta, String> colNombre;
    @FXML private TableColumn<DetalleVenta, Double> colPrecio;
    @FXML private TableColumn<DetalleVenta, Double> colCantidad;
    @FXML private TableColumn<DetalleVenta, Double> colSubtotal;

    private ItemDAO itemDAO;
    private ComboDAO comboDAO;
    private VentaDAO ventaDAO;
    private ObservableList<DetalleVenta> listaCarrito;
    private double totalVenta = 0.0;
    private ConfiguracionDAO configDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        itemDAO = new ItemDAO();
        comboDAO = new ComboDAO();
        ventaDAO = new VentaDAO();
        listaCarrito = FXCollections.observableArrayList();
        configDAO = new ConfiguracionDAO();

        rootPane.setStyle(ThemeService.getInstance().getBgStyle());

        configurarTabla();

        Platform.runLater(() -> txtBuscador.requestFocus());

        txtBuscador.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                buscarProducto();
            }
        });

        actualizarBotonEliminar(null);
        tablaDetalles.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            actualizarBotonEliminar(newSelection);
        });

        listaCarrito.addListener((javafx.collections.ListChangeListener<DetalleVenta>) c -> {
            if (btnVaciar != null) btnVaciar.setDisable(listaCarrito.isEmpty());
        });

        tablaDetalles.setRowFactory(tv -> {
            TableRow<DetalleVenta> fila = new TableRow<DetalleVenta>() {
                @Override
                protected void updateItem(DetalleVenta detalle, boolean empty) {
                    super.updateItem(detalle, empty);
                    if (detalle == null || empty) {
                        setStyle("");
                    } else {
                        if (verificarProblemaStock(detalle)) {
                            setStyle("-fx-background-color: #ffcdd2;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            };
            fila.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !fila.isEmpty()) {
                    ajustarCantidad(fila.getItem());
                }
            });
            return fila;
        });
    }

    private void actualizarBotonEliminar(DetalleVenta seleccion) {
        if (btnEliminar == null) return;

        if (seleccion == null) {
            btnEliminar.setDisable(true);
            btnEliminar.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #bdc3c7;");
        } else {
            btnEliminar.setDisable(false);
            btnEliminar.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoItem"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreItem"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCantidad.setCellFactory(col -> new javafx.scene.control.TableCell<DetalleVenta, Double>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                if (empty || valor == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                DetalleVenta d = getTableRow().getItem();
                String unidad = d.esCombo() ? "u" : d.getItem().getUnidad();
                setText(valor % 1 == 0
                        ? (int) valor.doubleValue() + " " + unidad
                        : valor + " " + unidad);
            }
        });
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        tablaDetalles.setItems(listaCarrito);

        tablaDetalles.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.DELETE) {
                eliminarFilaSeleccionada();
            }
        });
    }

    @FXML
    void agregarProductoManual(ActionEvent event) {
        buscarProducto();
    }

    private void buscarProducto() {
        String termino = txtBuscador.getText().trim();
        if (termino.isEmpty()) return;

        try {
            List<ItemVenta> items = itemDAO.listarTodos().stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(termino) ||
                             p.getNombre().toLowerCase().contains(termino.toLowerCase()))
                .collect(Collectors.toList());

            List<Combo> combos = comboDAO.listarTodos().stream()
                .filter(c -> c.getCodigo().equalsIgnoreCase(termino) ||
                             c.getNombre().toLowerCase().contains(termino.toLowerCase()))
                .collect(Collectors.toList());

            int total = items.size() + combos.size();

            if (total == 0) {
                AlertUtil.mostrar(Alert.AlertType.WARNING, "No encontrado", "No existe producto con ese criterio.");
            } else if (total == 1) {
                if (!items.isEmpty()) agregarAlCarrito(items.get(0));
                else agregarComboAlCarrito(combos.get(0));
                txtBuscador.clear();
            } else {
                List<ItemVenta> todos = new java.util.ArrayList<>(items);
                combos.forEach(c -> todos.add(ItemVenta.desdeCombo(c)));
                ChoiceDialog<ItemVenta> dialog = new ChoiceDialog<>(todos.get(0), todos);
                dialog.setTitle("Seleccionar Producto");
                dialog.setHeaderText("Múltiples coincidencias encontradas");
                dialog.setContentText("Elige el correcto:");
                dialog.showAndWait().ifPresent(iv -> {
                    if (iv.isEsCombo()) {
                        combos.stream().filter(c -> c.getId() == iv.getIdCombo()).findFirst()
                              .ifPresent(this::agregarComboAlCarrito);
                    } else {
                        agregarAlCarrito(iv);
                    }
                    txtBuscador.clear();
                });
            }

        } catch (SQLException e) {
            AlertUtil.mostrar(Alert.AlertType.ERROR, "Error BD", e.getMessage());
        }
    }

    private void agregarComboAlCarrito(Combo combo) {
        for (DetalleVenta d : listaCarrito) {
            if (d.esCombo() && d.getCombo().getId() == combo.getId()) {
                d.setCantidad(d.getCantidad() + 1);
                tablaDetalles.refresh();
                recalcularTotal();
                return;
            }
        }
        listaCarrito.add(new DetalleVenta(combo, 1));
        tablaDetalles.refresh();
        recalcularTotal();
    }

    private void agregarAlCarrito(ItemVenta item) {
        double cantidad;

        if (item.esPorPeso()) {
            TextInputDialog dialog = new TextInputDialog("1");
            dialog.setTitle("Cantidad");
            dialog.setHeaderText(item.getNombre() + " — se vende por " + item.getUnidad());
            dialog.setContentText("Ingresá la cantidad (" + item.getUnidad() + "):");

            Optional<String> resultado = dialog.showAndWait();
            if (resultado.isEmpty()) return;

            try {
                cantidad = Double.parseDouble(resultado.get().replace(",", "."));
                if (cantidad <= 0) {
                    AlertUtil.mostrar(Alert.AlertType.WARNING, "Cantidad inválida", "La cantidad debe ser mayor a cero.");
                    return;
                }
            } catch (NumberFormatException e) {
                AlertUtil.mostrar(Alert.AlertType.ERROR, "Error de formato", "Ingresá un número válido (Ej: 1.5)");
                return;
            }
        } else {
            cantidad = 1;
        }

        boolean encontrado = false;
        for (DetalleVenta d : listaCarrito) {
            if (!d.esCombo() && d.getItem().getId() == item.getId()) {
                d.setCantidad(d.getCantidad() + cantidad);
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            listaCarrito.add(new DetalleVenta(item, cantidad));
        }

        tablaDetalles.refresh();
        recalcularTotal();
    }

    @FXML
    void eliminarLinea(ActionEvent event) {
        eliminarFilaSeleccionada();
    }

    private void eliminarFilaSeleccionada() {
        DetalleVenta seleccionado = tablaDetalles.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            listaCarrito.remove(seleccionado);
            recalcularTotal();
            txtBuscador.requestFocus();
        } else {
            AlertUtil.mostrar(Alert.AlertType.WARNING, "Atención", "Selecciona un ítem de la lista para quitarlo.");
        }
    }

    private void ajustarCantidad(DetalleVenta detalle) {
        String unidad = detalle.esCombo() ? "u" : detalle.getItem().getUnidad();
        String cantidadActual = detalle.getCantidad() % 1 == 0
                ? String.valueOf((int) detalle.getCantidad())
                : String.valueOf(detalle.getCantidad());

        TextInputDialog dialog = new TextInputDialog(cantidadActual);
        dialog.setTitle("Ajustar cantidad");
        dialog.setHeaderText(detalle.getNombreItem());
        dialog.setContentText("Nueva cantidad (" + unidad + "):");

        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isEmpty()) return;

        try {
            double nuevaCantidad = Double.parseDouble(resultado.get().replace(",", "."));
            if (nuevaCantidad <= 0) {
                listaCarrito.remove(detalle);
            } else {
                detalle.setCantidad(nuevaCantidad);
                tablaDetalles.refresh();
            }
            recalcularTotal();
        } catch (NumberFormatException e) {
            AlertUtil.mostrar(Alert.AlertType.ERROR, "Error de formato", "Ingresá un número válido (Ej: 1.5)");
        }
    }

    @FXML
    void vaciarCarrito(ActionEvent event) {
        if (listaCarrito.isEmpty()) return;

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Vaciar carrito");
        confirmacion.setHeaderText("¿Vaciar todos los ítems?");
        confirmacion.setContentText("Se eliminarán " + listaCarrito.size() + " ítem(s) del carrito.");

        ButtonType btnConfirmar = new ButtonType("Vaciar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(btnConfirmar, btnCancelarBtn);

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == btnConfirmar) {
            limpiarPantalla();
        }
    }

    private void recalcularTotal() {
        totalVenta = 0.0;
        for (DetalleVenta d : listaCarrito) {
            totalVenta += d.getSubtotal();
        }
        lblTotal.setText(String.format("ARS %.2f", totalVenta));
        validarBotonCobrar();
    }

    @FXML
    void finalizarVenta(ActionEvent event) {
        if (listaCarrito.isEmpty()) return;

        try {
            Venta venta = new Venta();
            venta.setFecha(LocalDateTime.now().toString());
            for(DetalleVenta d : listaCarrito) venta.agregarDetalle(d);
            venta.calcularTotal();

            ventaDAO.registrarVenta(venta);

            TicketService ticketService = new TicketService();
            File ticketGenerado = ticketService.generarTicketPDF(venta);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Venta Exitosa");
            alert.setHeaderText("La venta #" + venta.getId() + " se registró correctamente.");
            alert.setContentText("¿Deseas ver o imprimir el ticket?");

            ButtonType btnImprimir = new ButtonType("🖨️ Ver Ticket", ButtonBar.ButtonData.YES);
            ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.NO);

            alert.getButtonTypes().setAll(btnImprimir, btnCerrar);

            Optional<ButtonType> resultado = alert.showAndWait();
            if (resultado.isPresent() && resultado.get() == btnImprimir) {
                ticketService.abrirArchivo(ticketGenerado);
            }

            limpiarPantalla();

        } catch (SQLException e) {
            AlertUtil.mostrar(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    void cancelarVenta(ActionEvent event) {
        ((Stage) txtBuscador.getScene().getWindow()).close();
    }

    private void limpiarPantalla() {
        listaCarrito.clear();
        recalcularTotal();
        txtBuscador.requestFocus();
    }


    private boolean verificarProblemaStock(DetalleVenta detalle) {
        try {
            Configuracion config = configDAO.obtenerConfiguracion();
            boolean permitirNegativo = (config != null) && config.isPermitirStockNegativo();
            if (permitirNegativo) return false;

            if (detalle.esCombo()) {
                int stockCombo = comboDAO.calcularStock(detalle.getCombo().getId());
                return detalle.getCantidad() > stockCombo;
            }

            if (detalle.getItem().isEsServicio()) return false;
            return detalle.getCantidad() > detalle.getItem().getStock();

        } catch (SQLException e) {
            return true;
        }
    }

    private void validarBotonCobrar() {
        if (btnCobrar == null) return;

        boolean hayErrores = false;

        for (DetalleVenta d : listaCarrito) {
            if (verificarProblemaStock(d)) {
                hayErrores = true;
                break;
            }
        }

        if (hayErrores || listaCarrito.isEmpty()) {
            btnCobrar.setDisable(true);
            btnCobrar.setText(hayErrores ? "STOCK INSUFICIENTE (!)" : "COBRAR (F12)");
            btnCobrar.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        } else {
            btnCobrar.setDisable(false);
            btnCobrar.setText("COBRAR (F12)");
            btnCobrar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);");
        }
    }
}
