package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ConfigController implements Initializable {

    @FXML private TextField txtNombreEmpresa;
    @FXML private TextField txtCuit;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtCondicionIva;
    @FXML private TextField txtPuntoVenta;
    
    private ConfiguracionDAO configDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configDAO = new ConfiguracionDAO();
        cargarDatos();
    }
    
    private void cargarDatos() {
        try {
            Configuracion config = configDAO.obtenerConfiguracion();
            if (config != null) {
                txtNombreEmpresa.setText(config.getNombreEmpresa());
                txtCuit.setText(config.getCuit());
                txtDireccion.setText(config.getDireccion());
                txtCondicionIva.setText(config.getCondicionIva());
                txtPuntoVenta.setText(String.valueOf(config.getPuntoVenta()));
            }
        } catch (SQLException e) {
            mostrarAlerta("Error al cargar configuración", e.getMessage());
        }
    }

    @FXML
    void guardarCambios(ActionEvent event) {
        try {
            // Validar punto de venta numérico
            int pv = Integer.parseInt(txtPuntoVenta.getText());
            
            Configuracion nuevaConfig = new Configuracion(
                txtNombreEmpresa.getText(),
                txtCuit.getText(),
                txtDireccion.getText(),
                txtCondicionIva.getText(),
                pv
            );
            
            configDAO.guardarConfiguracion(nuevaConfig);
            
            mostrarAlerta("Éxito", "Datos de la empresa actualizados correctamente.");
            cerrarVentana(event);
            
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El Punto de Venta debe ser un número.");
        } catch (SQLException e) {
            mostrarAlerta("Error BD", "No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        // Obtenemos el Stage desde cualquier control (ej. el campo de texto)
        Stage stage = (Stage) txtNombreEmpresa.getScene().getWindow();
        stage.close();
    }
    
    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}