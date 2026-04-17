package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.UsuarioDAO;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.model.Usuario.Rol;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import org.mindrot.jbcrypt.BCrypt;

public class AdminUsuariosController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, Rol> colRol;
    @FXML private TextField txtNombre;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Rol> cmbRol;
    @FXML private Button btnGuardar;
    @FXML private Button btnCambiarPassword;
    @FXML private Button btnCancelar;
    @FXML private Label lblFormTitulo;
    @FXML private Label lblMensaje;

    private UsuarioDAO usuarioDAO;
    private Usuario usuarioEnEdicion = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        usuarioDAO = new UsuarioDAO();

        rootPane.setStyle(ThemeService.getInstance().getBgStyle());

        cmbRol.setItems(FXCollections.observableArrayList(Rol.values()));
        cmbRol.setValue(Rol.CAJERO);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));

        colRol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Rol item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.name());
                setStyle(item == Rol.ADMIN
                    ? "-fx-text-fill: #2c3e50; -fx-font-weight: bold;"
                    : "-fx-text-fill: #7f8c8d;");
            }
        });

        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, ant, sel) -> {
            if (sel != null) entrarModoEdicion(sel);
        });

        cargarDatos();
    }

    private void entrarModoEdicion(Usuario usuario) {
        usuarioEnEdicion = usuario;
        txtNombre.setText(usuario.getNombre());
        txtPassword.clear();
        cmbRol.setValue(usuario.getRol());
        lblFormTitulo.setText("Editando: " + usuario.getNombre());
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);
        btnCambiarPassword.setVisible(true);
        btnCambiarPassword.setManaged(true);
        btnCancelar.setVisible(true);
        btnCancelar.setManaged(true);
        lblMensaje.setText("");
    }

    @FXML
    void guardarUsuario(ActionEvent event) {
        String nombre = txtNombre.getText().trim();
        String password = txtPassword.getText();

        if (nombre.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Completá nombre y contraseña.");
            return;
        }

        try {
            if (usuarioDAO.buscarPorNombre(nombre) != null) {
                lblMensaje.setText("Ya existe un usuario con ese nombre.");
                return;
            }
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            usuarioDAO.guardar(new Usuario(nombre, hash, cmbRol.getValue()));
            cargarDatos();
            limpiarFormulario();
            lblMensaje.setStyle("-fx-text-fill: #27ae60;");
            lblMensaje.setText("Usuario creado correctamente.");
        } catch (SQLException e) {
            lblMensaje.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    void cambiarPassword(ActionEvent event) {
        String nuevaPass = txtPassword.getText();
        if (nuevaPass.isEmpty()) {
            lblMensaje.setText("Ingresá la nueva contraseña.");
            return;
        }
        try {
            usuarioEnEdicion.setPasswordHash(BCrypt.hashpw(nuevaPass, BCrypt.gensalt()));
            usuarioEnEdicion.setRol(cmbRol.getValue());
            usuarioDAO.actualizar(usuarioEnEdicion);
            cargarDatos();
            limpiarFormulario();
            lblMensaje.setStyle("-fx-text-fill: #27ae60;");
            lblMensaje.setText("Usuario actualizado correctamente.");
        } catch (SQLException e) {
            lblMensaje.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    void eliminarUsuario(ActionEvent event) {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            lblMensaje.setStyle("-fx-text-fill: #e74c3c;");
            lblMensaje.setText("Seleccioná un usuario de la lista.");
            return;
        }

        Usuario activo = SessionService.getInstance().getUsuarioActivo();
        if (activo != null && seleccionado.getId() == activo.getId()) {
            lblMensaje.setStyle("-fx-text-fill: #e74c3c;");
            lblMensaje.setText("No podés eliminar tu propio usuario.");
            return;
        }

        try {
            List<Usuario> todos = usuarioDAO.listarTodos();
            long cantAdmins = todos.stream().filter(u -> u.getRol() == Rol.ADMIN).count();
            if (seleccionado.getRol() == Rol.ADMIN && cantAdmins <= 1) {
                lblMensaje.setStyle("-fx-text-fill: #e74c3c;");
                lblMensaje.setText("Debe existir al menos un administrador.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Eliminar usuario");
            confirm.setContentText("¿Eliminar a " + seleccionado.getNombre() + "?");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;

            usuarioDAO.eliminar(seleccionado.getId());
            cargarDatos();
            limpiarFormulario();
            lblMensaje.setStyle("-fx-text-fill: #27ae60;");
            lblMensaje.setText("Usuario eliminado.");
        } catch (SQLException e) {
            lblMensaje.setStyle("-fx-text-fill: #e74c3c;");
            lblMensaje.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    void cancelarEdicion(ActionEvent event) {
        limpiarFormulario();
    }

    private void cargarDatos() {
        try {
            tablaUsuarios.setItems(FXCollections.observableArrayList(usuarioDAO.listarTodos()));
        } catch (SQLException e) {
            lblMensaje.setText("Error al cargar usuarios: " + e.getMessage());
        }
    }

    private void limpiarFormulario() {
        usuarioEnEdicion = null;
        tablaUsuarios.getSelectionModel().clearSelection();
        txtNombre.clear();
        txtPassword.clear();
        cmbRol.setValue(Rol.CAJERO);
        lblFormTitulo.setText("Nuevo usuario:");
        lblMensaje.setText("");
        btnGuardar.setVisible(true);
        btnGuardar.setManaged(true);
        btnCambiarPassword.setVisible(false);
        btnCambiarPassword.setManaged(false);
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
    }
}
