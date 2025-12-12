package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class MainController implements Initializable {

    // --- TABLA ---
    @FXML
    private TableView<ItemVenta> tablaItems;
    @FXML
    private TableColumn<ItemVenta, Integer> colId;
    @FXML
    private TableColumn<ItemVenta, String> colCodigo; // Nuevo columna
    @FXML
    private TableColumn<ItemVenta, String> colNombre;
    @FXML
    private TableColumn<ItemVenta, Double> colPrecio;
    @FXML
    private TableColumn<ItemVenta, Double> colStock;

    // --- FORMULARIO ---
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TextField txtStock;
    @FXML
    private CheckBox chkServicio;

    private ItemDAO itemDAO;
    private ObservableList<ItemVenta> listaItems;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        itemDAO = new ItemDAO();
        configurarColumnas();
        cargarDatos();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
    }

    private void cargarDatos() {
        try {
            // Guardamos la lista en una variable global para poder modificarla luego
            listaItems = FXCollections.observableArrayList(itemDAO.listarTodos());
            tablaItems.setItems(listaItems);
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error DB", "No se pudo cargar la lista: " + e.getMessage());
        }
    }

    // --- MÉTODO DEL BOTÓN GUARDAR ---
    @FXML
    void guardarItem(ActionEvent event) {
        try {
            // 1. Validar datos mínimos
            if (txtCodigo.getText().isEmpty() || txtNombre.getText().isEmpty() || txtPrecio.getText().isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos", "Por favor llena Código, Nombre y Precio.");
                return;
            }

            // 2. Crear objeto (Parseando los números)
            ItemVenta nuevoItem = new ItemVenta();
            nuevoItem.setCodigo(txtCodigo.getText());
            nuevoItem.setNombre(txtNombre.getText());
            nuevoItem.setDescripcion(""); // Opcional por ahora
            nuevoItem.setPrecioCosto(0.0); // Opcional por ahora
            nuevoItem.setPrecioVenta(Double.parseDouble(txtPrecio.getText()));

            // Si es servicio, el stock es -1, si no, lo que diga la caja
            if (chkServicio.isSelected()) {
                nuevoItem.setEsServicio(true);
                nuevoItem.setStock(-1);
            } else {
                nuevoItem.setEsServicio(false);
                // Si la caja de stock está vacía, ponemos 0
                String stockStr = txtStock.getText().isEmpty() ? "0" : txtStock.getText();
                nuevoItem.setStock(Double.parseDouble(stockStr));
            }

            // 3. Guardar en Base de Datos
            itemDAO.guardar(nuevoItem);

            // 4. Refrescar la tabla (re-cargando todo o agregando a la lista)
            cargarDatos();
            limpiarFormulario();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto guardado correctamente.");

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Formato", "El Precio y Stock deben ser números válidos (usa punto para decimales).");
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error Base de Datos", "No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    void eliminarItem(ActionEvent event) {
        // 1. Obtener item seleccionado
        ItemVenta itemSeleccionado = tablaItems.getSelectionModel().getSelectedItem();

        if (itemSeleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Selecciona un producto de la lista para eliminar.");
            return;
        }

        // 2. CREAR LA CONFIRMACIÓN
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Estás seguro de que deseas eliminar: " + itemSeleccionado.getNombre() + "?");

        // 3. Mostrar y esperar respuesta
        Optional<ButtonType> resultado = confirmacion.showAndWait();

        // 4. Si el usuario dijo "OK", procedemos
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                itemDAO.eliminar(itemSeleccionado.getId());
                cargarDatos(); // Refrescar tabla
                mostrarAlerta(Alert.AlertType.INFORMATION, "Eliminado", "El producto fue eliminado correctamente.");
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo eliminar: " + e.getMessage());
            }
        } else {
            // El usuario canceló o cerró la ventana
            System.out.println("Eliminación cancelada por el usuario.");
        }
    }

    private void limpiarFormulario() {
        txtCodigo.clear();
        txtNombre.clear();
        txtPrecio.clear();
        txtStock.clear();
        chkServicio.setSelected(false);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
