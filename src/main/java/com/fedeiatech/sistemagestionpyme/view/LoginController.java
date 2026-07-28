package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.UsuarioDAO;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController implements Initializable {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setText("");
    }

    @FXML
    void iniciarSesion(ActionEvent event) {
        String nombre = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        if (nombre.isEmpty() || password.isEmpty()) {
            lblError.setText("Completá usuario y contraseña.");
            return;
        }

        try {
            Usuario usuario = new UsuarioDAO().autenticar(nombre, password);
            if (usuario == null) {
                lblError.setText("Usuario o contraseña incorrectos.");
                txtPassword.clear();
                return;
            }

            SessionService.getInstance().iniciarSesion(usuario);
            abrirDashboard();

        } catch (SQLException e) {
            lblError.setText("Error de base de datos: " + e.getMessage());
        } catch (IOException e) {
            lblError.setText("Error al cargar la aplicación.");
        }
    }

    private void abrirDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard_view.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) btnIngresar.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Sistema FedeiaTech - Pyme v0.9.0");
        stage.setMaximized(true);
    }
}
