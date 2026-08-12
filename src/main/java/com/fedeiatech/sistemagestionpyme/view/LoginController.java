package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.UsuarioDAO;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class LoginController implements Initializable {

    @FXML private ComboBox<String> cmbUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ConfiguracionDAO configDAO = new ConfiguracionDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setText("");
        try {
            cmbUsuario.setItems(FXCollections.observableArrayList(
                usuarioDAO.listarTodos().stream().map(Usuario::getNombre).toList()));
            String ultimoUsuario = configDAO.obtenerUltimoUsuario();
            if (ultimoUsuario != null && cmbUsuario.getItems().contains(ultimoUsuario)) {
                cmbUsuario.setValue(ultimoUsuario);
            } else if (!cmbUsuario.getItems().isEmpty()) {
                cmbUsuario.setValue(cmbUsuario.getItems().get(0));
            }
        } catch (SQLException e) {
            lblError.setText("Error al cargar usuarios: " + e.getMessage());
        }
        javafx.application.Platform.runLater(txtPassword::requestFocus);
    }

    @FXML
    void iniciarSesion(ActionEvent event) {
        String nombre = cmbUsuario.getValue();
        String password = txtPassword.getText();

        if (nombre == null || nombre.isBlank() || password.isEmpty()) {
            lblError.setText("Seleccioná un usuario y completá la contraseña.");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(nombre, password);
            if (usuario == null) {
                lblError.setText("Usuario o contraseña incorrectos.");
                txtPassword.clear();
                return;
            }

            SessionService.getInstance().iniciarSesion(usuario);
            configDAO.actualizarUltimoUsuario(usuario.getNombre());
            mostrarSplashYAbrirDashboard();

        } catch (SQLException e) {
            lblError.setText("Error de base de datos: " + e.getMessage());
        }
    }

    private void mostrarSplashYAbrirDashboard() {
        Stage stagePrincipal = (Stage) btnIngresar.getScene().getWindow();

        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/logo_ferrematica.png")));
        logo.setFitWidth(120.0);
        logo.setPreserveRatio(true);

        Label lblCargando = new Label("Cargando");
        lblCargando.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12;");

        VBox contenido = new VBox(20, logo, lblCargando);
        contenido.setAlignment(Pos.CENTER);
        contenido.setStyle("-fx-background-color: #2c3e50;");

        Stage splash = new Stage(StageStyle.UNDECORATED);
        splash.setScene(new Scene(contenido, 260, 260));
        splash.setResizable(false);
        splash.centerOnScreen();
        splash.show();
        stagePrincipal.hide();

        Timeline puntosSuspensivos = new Timeline(new KeyFrame(Duration.millis(400), e -> {
            int cantidad = (lblCargando.getText().length() - "Cargando".length() + 1) % 4;
            lblCargando.setText("Cargando" + ".".repeat(cantidad));
        }));
        puntosSuspensivos.setCycleCount(Timeline.INDEFINITE);
        puntosSuspensivos.play();

        PauseTransition espera = new PauseTransition(Duration.seconds(1.5));
        espera.setOnFinished(e -> {
            puntosSuspensivos.stop();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard_view.fxml"));
                Parent root = loader.load();
                stagePrincipal.getScene().setRoot(root);
                stagePrincipal.setTitle("Sistema FedeiaTech - Pyme v1.2.0");
                stagePrincipal.setResizable(true);
                stagePrincipal.setMaximized(true);
                stagePrincipal.show();
            } catch (IOException ex) {
                lblError.setText("Error al cargar la aplicación.");
                stagePrincipal.show();
            } finally {
                splash.close();
            }
        });
        espera.play();
    }
}
