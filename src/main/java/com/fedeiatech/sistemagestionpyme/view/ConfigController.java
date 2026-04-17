package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ConfigController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private TextField txtNombreEmpresa;
    @FXML private TextField txtCuit;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtCondicionIva;
    @FXML private TextField txtPuntoVenta;

    @FXML private ImageView imgLogoPreview;
    @FXML private Label lblRutaLogo;
    @FXML private TextArea txtMensajeTicket;

    @FXML private TextField txtRutaTickets;

    @FXML private CheckBox chkStockNegativo;
    @FXML private TextField txtRecargo;

    private ConfiguracionDAO configDAO;
    private File archivoLogoSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configDAO = new ConfiguracionDAO();
        rootPane.setStyle(ThemeService.getInstance().getBgStyle());
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

                lblRutaLogo.setText(config.getRutaLogo() != null ? config.getRutaLogo() : "");
                if (config.getRutaLogo() != null && !config.getRutaLogo().isEmpty()) {
                    File imgFile = new File(config.getRutaLogo());
                    if(imgFile.exists()) {
                        imgLogoPreview.setImage(new Image(imgFile.toURI().toString()));
                    }
                }
                txtMensajeTicket.setText(config.getMensajeTicket());

                txtRutaTickets.setText(config.getRutaGuardadoTickets() != null ? config.getRutaGuardadoTickets() : "");

                chkStockNegativo.setSelected(config.isPermitirStockNegativo());
                txtRecargo.setText(String.valueOf(config.getRecargoTarjeta()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void seleccionarLogo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Logotipo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(txtNombreEmpresa.getScene().getWindow());
        if (file != null) {
            archivoLogoSeleccionado = file;
            lblRutaLogo.setText(file.getAbsolutePath());
            imgLogoPreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    void seleccionarCarpetaTickets(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar carpeta para guardar Tickets");

        File selectedDirectory = directoryChooser.showDialog(txtNombreEmpresa.getScene().getWindow());

        if (selectedDirectory != null) {
            txtRutaTickets.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    void borrarRutaTickets(ActionEvent event) {
        txtRutaTickets.setText("");
    }

    @FXML
    void generarBackup(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Copia de Seguridad");
        fileChooser.setInitialFileName("gestion_pyme_backup.db");

        File destino = fileChooser.showSaveDialog(txtNombreEmpresa.getScene().getWindow());
        if (destino != null) {
            try {
                File origen = new File("gestion_pyme.db");
                if (origen.exists()) {
                    Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    mostrarAlerta("Backup Exitoso", "Copia guardada en: " + destino.getAbsolutePath());
                }
            } catch (IOException e) {
                mostrarAlerta("Error", "No se pudo crear el backup: " + e.getMessage());
            }
        }
    }

    @FXML
    void guardarCambios(ActionEvent event) {
        try {
            int pv = Integer.parseInt(txtPuntoVenta.getText());
            double recargo = Double.parseDouble(txtRecargo.getText());

            Configuracion configActual = configDAO.obtenerConfiguracion();

            Configuracion config = new Configuracion();
            config.setNombreEmpresa(txtNombreEmpresa.getText());
            config.setCuit(txtCuit.getText());
            config.setDireccion(txtDireccion.getText());
            config.setCondicionIva(txtCondicionIva.getText());
            config.setPuntoVenta(pv);

            config.setRutaLogo(lblRutaLogo.getText());
            config.setMensajeTicket(txtMensajeTicket.getText());
            config.setRutaGuardadoTickets(txtRutaTickets.getText());

            config.setPermitirStockNegativo(chkStockNegativo.isSelected());
            config.setRecargoTarjeta(recargo);

            if (configActual != null) {
                config.setCertificadoRuta(configActual.getCertificadoRuta());
                config.setRutaBackup(configActual.getRutaBackup());
            }

            configDAO.guardarConfiguracion(config);

            mostrarAlerta("Guardado", "Configuración actualizada correctamente.");
            cerrarVentana(event);

        } catch (Exception e) {
            mostrarAlerta("Error", "Verifica los datos ingresados. " + e.getMessage());
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        ((Stage) txtNombreEmpresa.getScene().getWindow()).close();
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    @FXML
    void restaurarBackup(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Copia de Seguridad para Restaurar");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de Datos SQLite", "*.db"));

        File origen = fileChooser.showOpenDialog(txtNombreEmpresa.getScene().getWindow());
        if (origen != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Peligro: Sobrescribir Datos");
            confirm.setHeaderText("¿Estás seguro de restaurar esta copia?");
            confirm.setContentText("Se borrarán TODOS los datos actuales y se reemplazarán por los de la copia.\n\nEl programa se cerrará automáticamente al finalizar.");

            if (confirm.showAndWait().get() == ButtonType.OK) {
                try {
                    File destino = new File("gestion_pyme.db");
                    Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    mostrarAlerta("Restauración Exitosa", "La base de datos ha sido restaurada.\nEl sistema se cerrará para aplicar cambios.");
                    System.exit(0);

                } catch (IOException e) {
                    mostrarAlerta("Error Crítico", "No se pudo restaurar (El archivo puede estar en uso). Intenta cerrar el programa y reemplazar el archivo 'gestion_pyme.db' manualmente.");
                }
            }
        }
    }
}
