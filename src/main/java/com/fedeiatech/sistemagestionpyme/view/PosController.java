package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.service.TicketService;
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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.geometry.Point2D; // <--- Importante para la lógica de coordenadas

public class PosController implements Initializable {

    @FXML private TextField txtBuscador;
    @FXML private Label lblTotal;
    @FXML private Button btnCobrar; 
    @FXML private Button btnEliminar; 
    @FXML private javafx.scene.layout.AnchorPane rootPane;
    
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
    private ConfiguracionDAO configDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        itemDAO = new ItemDAO();
        ventaDAO = new VentaDAO();
        listaCarrito = FXCollections.observableArrayList();
        configDAO = new ConfiguracionDAO();
        
        // 1. Configuración de filas rojas (Stock)
        tablaDetalles.setRowFactory(tv -> new TableRow<DetalleVenta>() {
            @Override
            protected void updateItem(DetalleVenta detalle, boolean empty) {
                super.updateItem(detalle, empty);
                if (detalle == null || empty) {
                    setStyle("");
                } else {
                    boolean stockProblematico = verificarProblemaStock(detalle);
                    if (stockProblematico) {
                        setStyle("-fx-background-color: #ffcdd2;"); // Rojo claro
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        configurarTabla();
        
        // 2. Foco inicial
        Platform.runLater(() -> txtBuscador.requestFocus());
        
        txtBuscador.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                buscarProducto();
            }
        });
        
        // 3. Lógica de Deselección (MÉTODO MATEMÁTICO - MÁS ROBUSTO)
        // Esperamos a que la Scene cambie (se cargue) para agregar el filtro
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    // Convertimos la coordenada del clic (Scene X,Y) a coordenadas locales de cada control
                    // y preguntamos: "¿El clic cayó dentro de ti?"
                    
                    boolean clicEnTabla = estaDentro(tablaDetalles, event);
                    boolean clicEnBotonEliminar = estaDentro(btnEliminar, event);
                    boolean clicEnBotonCobrar = estaDentro(btnCobrar, event);
                    boolean clicEnBuscador = estaDentro(txtBuscador, event);

                    // Si el clic NO fue en la tabla y NO fue en los botones de acción...
                    if (!clicEnTabla && !clicEnBotonEliminar && !clicEnBotonCobrar && !clicEnBuscador) {
                        tablaDetalles.getSelectionModel().clearSelection();
                    }
                });
            }
        });
        
        // 4. Lógica visual del botón ELIMINAR
        actualizarBotonEliminar(null);
        tablaDetalles.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            actualizarBotonEliminar(newSelection);
        });
    }
    
    // Método auxiliar para verificar coordenadas
    private boolean estaDentro(Node nodo, MouseEvent event) {
        if (nodo == null || !nodo.isVisible()) return false;
        // Convierte el punto del clic a la coordenada del nodo y verifica si está dentro
        Point2D puntoLocal = nodo.sceneToLocal(event.getSceneX(), event.getSceneY());
        return nodo.contains(puntoLocal);
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
            List<ItemVenta> resultados = itemDAO.listarTodos().stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(termino) || 
                             p.getNombre().toLowerCase().contains(termino.toLowerCase()))
                .collect(Collectors.toList());

            if (resultados.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado", "No existe producto con ese criterio.");
            } 
            else if (resultados.size() == 1) {
                agregarAlCarrito(resultados.get(0));
                txtBuscador.clear();
            } 
            else {
                seleccionarDeLista(resultados);
            }

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error BD", e.getMessage());
        }
    }

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
        boolean encontrado = false;
        for (DetalleVenta d : listaCarrito) {
            if (d.getItem().getId() == item.getId()) {
                d.setCantidad(d.getCantidad() + 1);
                encontrado = true;
                break;
            }
        }
        
        if (!encontrado) {
            listaCarrito.add(new DetalleVenta(item, 1));
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
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Selecciona un ítem de la lista para quitarlo.");
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
            
            // 1. Generar Ticket (Sin abrirlo)
            TicketService ticketService = new TicketService();
            File ticketGenerado = ticketService.generarTicketPDF(venta);

            // 2. Mostrar Alerta Personalizada
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Venta Exitosa");
            alert.setHeaderText("La venta #" + venta.getId() + " se registró correctamente.");
            alert.setContentText("¿Deseas ver o imprimir el ticket?");

            // Definir botones
            ButtonType btnImprimir = new ButtonType("🖨️ Ver Ticket", ButtonBar.ButtonData.YES);
            ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.NO);

            alert.getButtonTypes().setAll(btnImprimir, btnCerrar);

            // Esperar respuesta
            Optional<ButtonType> resultado = alert.showAndWait();
            if (resultado.isPresent() && resultado.get() == btnImprimir) {
                ticketService.abrirArchivo(ticketGenerado);
            }

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
    
    private boolean verificarProblemaStock(DetalleVenta detalle) {
        try {
            if (detalle.getItem().isEsServicio()) return false;

            Configuracion config = configDAO.obtenerConfiguracion();
            boolean permitirNegativo = (config != null) && config.isPermitirStockNegativo();

            if (permitirNegativo) return false; 

            double stockReal = detalle.getItem().getStock();
            double cantidadSolicitada = detalle.getCantidad();

            return cantidadSolicitada > stockReal; 

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