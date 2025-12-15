package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
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
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class PosController implements Initializable {

    @FXML private TextField txtBuscador;
    @FXML private Label lblTotal;
    
    @FXML private TableView<DetalleVenta> tablaDetalles;
    @FXML private TableColumn<DetalleVenta, String> colCodigo;
    @FXML private TableColumn<DetalleVenta, String> colNombre;
    @FXML private TableColumn<DetalleVenta, Double> colPrecio;
    @FXML private TableColumn<DetalleVenta, Double> colCantidad;
    @FXML private TableColumn<DetalleVenta, Double> colSubtotal;

    private ItemDAO itemDAO;
    private VentaDAO ventaDAO;
    private ObservableList<DetalleVenta> listaCarrito;
    private double totalVenta = 0.0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        itemDAO = new ItemDAO();
        ventaDAO = new VentaDAO();
        listaCarrito = FXCollections.observableArrayList();
        
        configurarTabla();
        
        Platform.runLater(() -> txtBuscador.requestFocus());
        
        txtBuscador.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                buscarProducto();
            }
        });
    }
    
    private void configurarTabla() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoItem"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreItem"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal")); 
        
        tablaDetalles.setItems(listaCarrito);
        
        // NUEVO: Permitir borrar con la tecla SUPR (Delete)
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
    
    // --- LÓGICA DE BÚSQUEDA MEJORADA ---
    private void buscarProducto() {
        String termino = txtBuscador.getText().trim();
        if (termino.isEmpty()) return;

        try {
            // 1. Obtenemos TODOS los que coincidan (no solo el primero)
            List<ItemVenta> resultados = itemDAO.listarTodos().stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(termino) || 
                             p.getNombre().toLowerCase().contains(termino.toLowerCase()))
                .collect(Collectors.toList());

            if (resultados.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado", "No existe producto con ese criterio.");
            } 
            else if (resultados.size() == 1) {
                // CASO PERFECTO: Solo uno coincide (ej. escaneaste código exacto) -> Agregar directo
                agregarAlCarrito(resultados.get(0));
                txtBuscador.clear();
            } 
            else {
                // CASO AMBIGUO: Hay varios (ej. escribiste "Coca") -> Mostrar Selector
                seleccionarDeLista(resultados);
            }

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error BD", e.getMessage());
        }
    }

    // NUEVO: Diálogo para elegir entre varios productos
    private void seleccionarDeLista(List<ItemVenta> opciones) {
        ChoiceDialog<ItemVenta> dialog = new ChoiceDialog<>(opciones.get(0), opciones);
        dialog.setTitle("Seleccionar Producto");
        dialog.setHeaderText("Múltiples coincidencias encontradas");
        dialog.setContentText("Elige el correcto:");

        Optional<ItemVenta> result = dialog.showAndWait();
        result.ifPresent(item -> {
            agregarAlCarrito(item);
            txtBuscador.clear();
        });
    }

    private void agregarAlCarrito(ItemVenta item) {
        // Verificar si ya está para sumar cantidad
        for (DetalleVenta d : listaCarrito) {
            if (d.getItem().getId() == item.getId()) {
                d.setCantidad(d.getCantidad() + 1);
                tablaDetalles.refresh();
                recalcularTotal();
                return;
            }
        }
        // Si no, agregar nuevo
        listaCarrito.add(new DetalleVenta(item, 1));
        recalcularTotal();
    }
    
    // NUEVO: Eliminar item del carrito
    @FXML
    void eliminarLinea(ActionEvent event) {
        eliminarFilaSeleccionada();
    }

    private void eliminarFilaSeleccionada() {
        DetalleVenta seleccionado = tablaDetalles.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            listaCarrito.remove(seleccionado);
            recalcularTotal();
            txtBuscador.requestFocus(); // Devolver foco al buscador para seguir rápido
        } else {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Selecciona un ítem de la lista para quitarlo.");
        }
    }
    
    private void recalcularTotal() {
        totalVenta = 0.0;
        for (DetalleVenta d : listaCarrito) {
            totalVenta += d.getSubtotal();
        }
        lblTotal.setText(String.format("ARS %.2f", totalVenta));
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

            mostrarAlerta(Alert.AlertType.INFORMATION, "Venta Exitosa", "ID Venta: " + venta.getId());
            limpiarPantalla();
            
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", e.getMessage());
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

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}