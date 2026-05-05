package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ComboDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.model.Combo;
import com.fedeiatech.sistemagestionpyme.model.ComponenteCombo;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.net.URL;
import java.sql.SQLException;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ComboFormController implements Initializable {

    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtBuscarItem;
    @FXML private TextField txtCantComp;
    @FXML private TableView<ComponenteCombo> tablaComponentes;
    @FXML private TableColumn<ComponenteCombo, String> colCompCodigo;
    @FXML private TableColumn<ComponenteCombo, String> colCompNombre;
    @FXML private TableColumn<ComponenteCombo, Double> colCompCantidad;
    @FXML private TableColumn<ComponenteCombo, String> colCompAccion;
    @FXML private Label lblStock;
    @FXML private Button btnEliminar;

    private final ComboDAO comboDAO = new ComboDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private ObservableList<ComponenteCombo> componentes = FXCollections.observableArrayList();
    private int idComboEdicion = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colCompCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoItem"));
        colCompNombre.setCellValueFactory(new PropertyValueFactory<>("nombreItem"));
        colCompCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        colCompAccion.setCellFactory(col -> new TableCell<ComponenteCombo, String>() {
            private final Button btn = new Button("Quitar");
            {
                btn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 10;");
                btn.setOnAction(e -> {
                    ComponenteCombo comp = getTableView().getItems().get(getIndex());
                    componentes.remove(comp);
                    actualizarStock();
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tablaComponentes.setItems(componentes);
    }

    public void cargarCombo(int idCombo) {
        try {
            Combo combo = comboDAO.buscarPorCodigo(null);
            // buscar por id directo
            List<Combo> todos = comboDAO.listarTodos();
            combo = todos.stream().filter(c -> c.getId() == idCombo).findFirst().orElse(null);
            if (combo == null) return;

            idComboEdicion = combo.getId();
            txtCodigo.setText(combo.getCodigo());
            txtNombre.setText(combo.getNombre());
            txtDescripcion.setText(combo.getDescripcion() != null ? combo.getDescripcion() : "");
            txtPrecio.setText(String.valueOf(combo.getPrecioVenta()));
            componentes.setAll(combo.getComponentes());
            actualizarStock();
            btnEliminar.setVisible(true);
            btnEliminar.setManaged(true);
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo cargar el combo: " + e.getMessage());
        }
    }

    @FXML
    void agregarComponente(ActionEvent event) {
        String termino = txtBuscarItem.getText().trim();
        if (termino.isEmpty()) return;

        try {
            List<ItemVenta> todos = itemDAO.listarTodos();
            List<ItemVenta> resultados = todos.stream()
                .filter(i -> i.getCodigo().equalsIgnoreCase(termino)
                          || i.getNombre().toLowerCase().contains(termino.toLowerCase()))
                .filter(i -> !i.isEsServicio())
                .toList();

            if (resultados.isEmpty()) {
                mostrarAlerta("No encontrado", "No se encontró ningún producto con ese término.");
                return;
            }

            ItemVenta elegido;
            if (resultados.size() == 1) {
                elegido = resultados.get(0);
            } else {
                ChoiceDialog<ItemVenta> dialog = new ChoiceDialog<>(resultados.get(0), resultados);
                dialog.setTitle("Seleccionar producto");
                dialog.setHeaderText("Varios productos encontrados:");
                Optional<ItemVenta> res = dialog.showAndWait();
                if (res.isEmpty()) return;
                elegido = res.get();
            }

            double cantidad = 1.0;
            try {
                String cantStr = txtCantComp.getText().trim();
                if (!cantStr.isEmpty()) cantidad = Double.parseDouble(cantStr);
            } catch (NumberFormatException ignored) {}

            if (cantidad <= 0) {
                mostrarAlerta("Cantidad inválida", "La cantidad debe ser mayor a 0.");
                return;
            }

            final ItemVenta item = elegido;
            ComponenteCombo existente = componentes.stream()
                .filter(c -> c.getIdItem() == item.getId()).findFirst().orElse(null);

            if (existente != null) {
                existente.setCantidad(existente.getCantidad() + cantidad);
                tablaComponentes.refresh();
            } else {
                componentes.add(new ComponenteCombo(elegido.getId(), elegido.getCodigo(), elegido.getNombre(), cantidad));
            }

            txtBuscarItem.clear();
            txtCantComp.clear();
            actualizarStock();

        } catch (SQLException e) {
            mostrarAlerta("Error", e.getMessage());
        }
    }

    @FXML
    void guardar(ActionEvent event) {
        if (txtCodigo.getText().isBlank() || txtNombre.getText().isBlank() || txtPrecio.getText().isBlank()) {
            mostrarAlerta("Datos incompletos", "Completá Código, Nombre y Precio.");
            return;
        }
        if (componentes.isEmpty()) {
            mostrarAlerta("Sin componentes", "Agregá al menos un componente al combo.");
            return;
        }
        try {
            Combo combo = new Combo();
            combo.setId(idComboEdicion);
            combo.setCodigo(txtCodigo.getText().trim());
            combo.setNombre(txtNombre.getText().trim());
            combo.setDescripcion(txtDescripcion.getText().trim());
            combo.setPrecioVenta(Double.parseDouble(txtPrecio.getText().trim()));
            combo.setComponentes(new java.util.ArrayList<>(componentes));

            if (idComboEdicion > 0) {
                comboDAO.actualizar(combo);
            } else {
                comboDAO.guardar(combo);
            }
            cerrar();
        } catch (NumberFormatException e) {
            mostrarAlerta("Error de formato", "El precio debe ser un número válido.");
        } catch (SQLException e) {
            mostrarAlerta("Error DB", "No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    void eliminarCombo(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar combo");
        confirm.setContentText("¿Eliminar el combo " + txtNombre.getText() + "?");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    comboDAO.eliminar(idComboEdicion);
                    cerrar();
                } catch (SQLException e) {
                    mostrarAlerta("Error", "No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    void cancelar(ActionEvent event) { cerrar(); }

    private void actualizarStock() {
        if (componentes.isEmpty() || idComboEdicion == 0) {
            lblStock.setText("Stock calculado: (guardar para calcular)");
            return;
        }
        try {
            int stock = comboDAO.calcularStock(idComboEdicion);
            lblStock.setText("Stock calculado: " + stock + " combos disponibles");
            lblStock.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + (stock > 0 ? "#27ae60;" : "#e74c3c;"));
        } catch (SQLException e) {
            lblStock.setText("Stock calculado: error");
        }
    }

    private void cerrar() {
        ((Stage) txtCodigo.getScene().getWindow()).close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(titulo);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
