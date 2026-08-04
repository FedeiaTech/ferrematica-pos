package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.dao.UsuarioDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import com.fedeiatech.sistemagestionpyme.service.SupabaseSyncService;
import com.fedeiatech.sistemagestionpyme.service.SyncBloqueadoException;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import com.fedeiatech.sistemagestionpyme.view.util.AlertUtil;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ConfigController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ConfigController.class.getName());

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
    @FXML private Label lblInfoTickets;

    @FXML private CheckBox chkStockNegativo;
    @FXML private TextField txtRecargo;

    // Nuevos — Personalización
    @FXML private ChoiceBox<String> cmbAnchoTicket;
    @FXML private CheckBox chkMostrarDireccion;
    @FXML private CheckBox chkMostrarCuit;
    @FXML private CheckBox chkUsarEnteros;
    @FXML private TextField txtMargenGanancia;

    // Sincronización con Supabase (ADMIN-only)
    @FXML private Tab tabSyncSupabase;
    @FXML private VBox vboxSyncSupabase;
    @FXML private TextField txtSupabaseUrl;
    @FXML private TextField txtSupabaseAnonKey;
    @FXML private TextField txtSupabaseSyncEmail;
    @FXML private PasswordField txtSupabaseSyncPassword;
    @FXML private TextField txtSupabaseSyncIntervaloMin;
    @FXML private CheckBox chkSupabaseSyncHabilitado;
    @FXML private Label lblSyncBloqueado;
    @FXML private Button btnSincronizarAhora;

    private ConfiguracionDAO configDAO;
    private File archivoLogoSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configDAO = new ConfiguracionDAO();
        rootPane.setStyle(ThemeService.getInstance().getBgStyle());
        if (cmbAnchoTicket != null) {
            cmbAnchoTicket.getItems().addAll("58 mm", "80 mm");
        }
        cargarDatos();
        configurarVisibilidadSync();
    }

    private void configurarVisibilidadSync() {
        if (tabSyncSupabase == null) return;
        if (!SessionService.getInstance().esAdmin()) {
            tabSyncSupabase.setDisable(true);
            TabPane tabPane = (TabPane) tabSyncSupabase.getTabPane();
            if (tabPane != null) tabPane.getTabs().remove(tabSyncSupabase);
            return;
        }
        actualizarEstadoBloqueoSync();
    }

    private void actualizarEstadoBloqueoSync() {
        if (lblSyncBloqueado == null) return;
        try {
            List<String> bloqueados = new ItemDAO().validarCodigosParaSync();
            if (bloqueados.isEmpty()) {
                lblSyncBloqueado.setText("");
                lblSyncBloqueado.setVisible(false);
                lblSyncBloqueado.setManaged(false);
                if (chkSupabaseSyncHabilitado != null) chkSupabaseSyncHabilitado.setDisable(false);
                if (btnSincronizarAhora != null) btnSincronizarAhora.setDisable(false);
            } else {
                StringBuilder sb = new StringBuilder("Sincronización bloqueada: corregí estos productos en Inventario (código vacío o duplicado):\n");
                for (String linea : bloqueados) {
                    sb.append("• ").append(linea).append("\n");
                }
                lblSyncBloqueado.setText(sb.toString());
                lblSyncBloqueado.setVisible(true);
                lblSyncBloqueado.setManaged(true);
                if (chkSupabaseSyncHabilitado != null) {
                    chkSupabaseSyncHabilitado.setSelected(false);
                    chkSupabaseSyncHabilitado.setDisable(true);
                }
                if (btnSincronizarAhora != null) btnSincronizarAhora.setDisable(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al validar códigos para sincronización", e);
        }
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
                    if (imgFile.exists()) imgLogoPreview.setImage(new Image(imgFile.toURI().toString()));
                }
                txtMensajeTicket.setText(config.getMensajeTicket());

                String ruta = config.getRutaGuardadoTickets();
                txtRutaTickets.setText(ruta != null ? ruta : "");
                actualizarInfoTickets(ruta);

                chkStockNegativo.setSelected(config.isPermitirStockNegativo());
                txtRecargo.setText(String.valueOf(config.getRecargoTarjeta()));

                // Nuevos campos
                if (cmbAnchoTicket != null) {
                    cmbAnchoTicket.setValue(config.getAnchoTicketMm() == 58 ? "58 mm" : "80 mm");
                }
                if (chkMostrarDireccion != null) chkMostrarDireccion.setSelected(config.isTicketMostrarDireccion());
                if (chkMostrarCuit != null) chkMostrarCuit.setSelected(config.isTicketMostrarCuit());
                if (chkUsarEnteros != null) chkUsarEnteros.setSelected(config.isUsarEnteros());
                if (txtMargenGanancia != null) txtMargenGanancia.setText(
                    config.getMargenGananciaPct() > 0 ? String.valueOf(config.getMargenGananciaPct()) : "");

                // Sincronización con Supabase
                if (txtSupabaseUrl != null) txtSupabaseUrl.setText(config.getSupabaseUrl() != null ? config.getSupabaseUrl() : "");
                if (txtSupabaseAnonKey != null) txtSupabaseAnonKey.setText(config.getSupabaseAnonKey() != null ? config.getSupabaseAnonKey() : "");
                if (txtSupabaseSyncEmail != null) txtSupabaseSyncEmail.setText(config.getSupabaseSyncEmail() != null ? config.getSupabaseSyncEmail() : "");
                if (txtSupabaseSyncPassword != null) txtSupabaseSyncPassword.setText(config.getSupabaseSyncPassword() != null ? config.getSupabaseSyncPassword() : "");
                if (txtSupabaseSyncIntervaloMin != null) txtSupabaseSyncIntervaloMin.setText(String.valueOf(config.getSupabaseSyncIntervaloMin()));
                if (chkSupabaseSyncHabilitado != null) chkSupabaseSyncHabilitado.setSelected(config.isSupabaseSyncHabilitado());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar la configuración", e);
        }
    }

    private void actualizarInfoTickets(String ruta) {
        if (lblInfoTickets == null) return;
        if (ruta == null || ruta.isBlank()) {
            lblInfoTickets.setText("Modo temporal: los tickets se guardan en la carpeta temp del sistema y se eliminan al reiniciar el equipo o cuando el SO los limpie (normalmente en días o semanas).");
            lblInfoTickets.setStyle("-fx-text-fill: #e67e22;");
        } else {
            lblInfoTickets.setText("Los tickets se guardan permanentemente en: " + ruta);
            lblInfoTickets.setStyle("-fx-text-fill: #27ae60;");
        }
    }

    @FXML
    void seleccionarLogo(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar Logotipo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Imágenes (PNG, JPG, BMP, TIFF)", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tiff", "*.tif"));
        File file = fc.showOpenDialog(txtNombreEmpresa.getScene().getWindow());
        if (file != null) {
            archivoLogoSeleccionado = file;
            lblRutaLogo.setText(file.getAbsolutePath());
            try {
                imgLogoPreview.setImage(new Image(file.toURI().toString()));
            } catch (Exception ex) {
                // TIFF no tiene preview nativo en JavaFX, pero OpenPDF sí lo imprime
                lblRutaLogo.setText(file.getAbsolutePath() + " (sin vista previa — se imprimirá correctamente)");
            }
        }
    }

    @FXML
    void seleccionarCarpetaTickets(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar carpeta para guardar Tickets");
        File dir = dc.showDialog(txtNombreEmpresa.getScene().getWindow());
        if (dir != null) {
            txtRutaTickets.setText(dir.getAbsolutePath());
            actualizarInfoTickets(dir.getAbsolutePath());
        }
    }

    @FXML
    void borrarRutaTickets(ActionEvent event) {
        txtRutaTickets.setText("");
        actualizarInfoTickets(null);
    }

    @FXML
    void abrirCarpetaTickets(ActionEvent event) {
        String ruta = txtRutaTickets.getText();
        File carpeta = (ruta != null && !ruta.isBlank()) ? new File(ruta) : new File(System.getProperty("java.io.tmpdir"));
        try {
            if (carpeta.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(carpeta);
        } catch (IOException e) {
            AlertUtil.mostrarInfo("Error", "No se pudo abrir la carpeta: " + e.getMessage());
        }
    }

    @FXML
    void vaciarTicketsTemporales(ActionEvent event) {
        String ruta = txtRutaTickets.getText();
        File carpeta = (ruta != null && !ruta.isBlank()) ? new File(ruta) : new File(System.getProperty("java.io.tmpdir"));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vaciar tickets");
        confirm.setContentText("¿Eliminar los archivos PDF de tickets en\n" + carpeta.getAbsolutePath() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        int eliminados = 0;
        File[] archivos = carpeta.listFiles((d, name) -> name.startsWith("ticket_") || name.startsWith("Ticket_"));
        if (archivos != null) {
            for (File f : archivos) { if (f.delete()) eliminados++; }
        }
        AlertUtil.mostrarInfo("Limpieza completada", eliminados + " ticket(s) eliminados de " + carpeta.getAbsolutePath());
    }

    @FXML
    void abrirCarpetaDB(ActionEvent event) {
        File db = new File("gestion_pyme.db").getAbsoluteFile();
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(db.getParentFile());
        } catch (IOException e) {
            AlertUtil.mostrarInfo("Ubicación de la base de datos", db.getAbsolutePath());
        }
    }

    @FXML
    void limpiarTemporalesAntiguos(ActionEvent event) {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        long limite = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
        File[] viejos = tmpDir.listFiles((d, name) ->
            (name.startsWith("ticket_venta_") || name.startsWith("Ticket_")) && new File(d, name).lastModified() < limite);
        int eliminados = 0;
        if (viejos != null) { for (File f : viejos) { if (f.delete()) eliminados++; } }
        AlertUtil.mostrarInfo("Limpieza completada", eliminados + " ticket(s) temporales eliminados (antiguos de +30 días).");
    }

    @FXML
    void generarBackup(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Copia de Seguridad");
        fc.setInitialFileName("gestion_pyme_backup.db");
        File destino = fc.showSaveDialog(txtNombreEmpresa.getScene().getWindow());
        if (destino != null) {
            try {
                Files.copy(new File("gestion_pyme.db").toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                AlertUtil.mostrarInfo("Backup Exitoso", "Copia guardada en: " + destino.getAbsolutePath());
            } catch (IOException e) {
                AlertUtil.mostrarInfo("Error", "No se pudo crear el backup: " + e.getMessage());
            }
        }
    }

    @FXML
    void guardarCambios(ActionEvent event) {
        try {
            guardarConfiguracionDesdeFormulario();
            AlertUtil.mostrarInfo("Guardado", "Configuración actualizada correctamente.");
            cerrarVentana(event);
        } catch (Exception e) {
            AlertUtil.mostrarInfo("Error", "Verifica los datos ingresados: " + e.getMessage());
        }
    }

    private void guardarConfiguracionDesdeFormulario() throws Exception {
        int pv = Integer.parseInt(txtPuntoVenta.getText());
        double recargo = Double.parseDouble(txtRecargo.getText().replace(",", "."));

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
            config.setColorTema(configActual.getColorTema());
        }

        // Nuevos campos
        if (cmbAnchoTicket != null) {
            config.setAnchoTicketMm(cmbAnchoTicket.getValue() != null && cmbAnchoTicket.getValue().startsWith("58") ? 58 : 80);
        }
        config.setTicketMostrarDireccion(chkMostrarDireccion != null && chkMostrarDireccion.isSelected());
        config.setTicketMostrarCuit(chkMostrarCuit != null && chkMostrarCuit.isSelected());
        config.setUsarEnteros(chkUsarEnteros != null && chkUsarEnteros.isSelected());
        if (txtMargenGanancia != null && !txtMargenGanancia.getText().isBlank()) {
            config.setMargenGananciaPct(Double.parseDouble(txtMargenGanancia.getText().replace(",", ".")));
        }

        // Sincronización con Supabase
        if (txtSupabaseUrl != null) config.setSupabaseUrl(txtSupabaseUrl.getText());
        if (txtSupabaseAnonKey != null) config.setSupabaseAnonKey(txtSupabaseAnonKey.getText());
        if (txtSupabaseSyncEmail != null) config.setSupabaseSyncEmail(txtSupabaseSyncEmail.getText());
        if (txtSupabaseSyncPassword != null) config.setSupabaseSyncPassword(txtSupabaseSyncPassword.getText());
        if (txtSupabaseSyncIntervaloMin != null && !txtSupabaseSyncIntervaloMin.getText().isBlank()) {
            config.setSupabaseSyncIntervaloMin(Integer.parseInt(txtSupabaseSyncIntervaloMin.getText().trim()));
        } else {
            config.setSupabaseSyncIntervaloMin(15);
        }
        config.setSupabaseSyncHabilitado(chkSupabaseSyncHabilitado != null && chkSupabaseSyncHabilitado.isSelected());

        configDAO.guardarConfiguracion(config);
        SupabaseSyncService.getInstance().iniciarProgramacionSiCorresponde();
    }

    @FXML
    void sincronizarAhora(ActionEvent event) {
        if (chkSupabaseSyncHabilitado == null || !chkSupabaseSyncHabilitado.isSelected()) {
            AlertUtil.mostrarInfo("Sincronización deshabilitada",
                    "Habilitá la sincronización y guardá los cambios antes de sincronizar manualmente.");
            return;
        }
        try {
            guardarConfiguracionDesdeFormulario();
        } catch (Exception e) {
            AlertUtil.mostrarInfo("Error", "Verifica los datos ingresados: " + e.getMessage());
            return;
        }
        if (btnSincronizarAhora != null) btnSincronizarAhora.setDisable(true);
        new Thread(() -> {
            try {
                SupabaseSyncService.getInstance().sincronizar();
                javafx.application.Platform.runLater(() -> {
                    AlertUtil.mostrarInfo("Sincronización", "Sincronización con Supabase completada correctamente.");
                    if (btnSincronizarAhora != null) btnSincronizarAhora.setDisable(false);
                });
            } catch (SyncBloqueadoException e) {
                javafx.application.Platform.runLater(() -> {
                    AlertUtil.mostrarInfo("Sincronización bloqueada", e.getMessage());
                    if (btnSincronizarAhora != null) btnSincronizarAhora.setDisable(false);
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al sincronizar con Supabase", e);
                javafx.application.Platform.runLater(() -> {
                    AlertUtil.mostrarInfo("Error", "No se pudo sincronizar con Supabase: " + e.getMessage());
                    if (btnSincronizarAhora != null) btnSincronizarAhora.setDisable(false);
                });
            }
        }, "supabase-sync-manual").start();
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        ((Stage) txtNombreEmpresa.getScene().getWindow()).close();
    }

    @FXML
    void restaurarBackup(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar Copia de Seguridad para Restaurar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de Datos SQLite", "*.db"));
        File origen = fc.showOpenDialog(txtNombreEmpresa.getScene().getWindow());
        if (origen != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Peligro: Sobrescribir Datos");
            confirm.setHeaderText("¿Estás seguro de restaurar esta copia?");
            confirm.setContentText("Se borrarán TODOS los datos actuales. El programa se cerrará al finalizar.");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    File destino = new File("gestion_pyme.db").getAbsoluteFile();
                    Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    AlertUtil.mostrarInfo("Restauración Exitosa",
                        "La base de datos fue restaurada en:\n" + destino.getAbsolutePath() +
                        "\n\nEl sistema se cerrará para aplicar los cambios.");
                    System.exit(0);
                } catch (IOException e) {
                    AlertUtil.mostrarInfo("Error", "No se pudo restaurar: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    void borrarTodasLasVentas(ActionEvent event) {
        // Paso 1: solicitar contraseña del admin
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("Contraseña del administrador");
        Dialog<ButtonType> dlgPass = new Dialog<>();
        dlgPass.setTitle("Confirmar identidad");
        dlgPass.setHeaderText("Ingresá la contraseña del administrador para continuar.");
        dlgPass.getDialogPane().setContent(pfPass);
        dlgPass.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dlgPass.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            String nombreAdmin = SessionService.getInstance().getUsuarioActivo().getNombre();
            Usuario verificado = new UsuarioDAO().autenticar(nombreAdmin, pfPass.getText());
            if (verificado == null) {
                AlertUtil.mostrarInfo("Contraseña incorrecta", "La contraseña ingresada no es válida.");
                return;
            }
        } catch (Exception e) {
            AlertUtil.mostrarInfo("Error", "No se pudo verificar la identidad: " + e.getMessage());
            return;
        }

        // Paso 2: confirmar la acción destructiva
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Borrar historial de ventas");
        confirm.setHeaderText("Esta acción es IRREVERSIBLE.");
        confirm.setContentText(
            "Se eliminarán TODAS las ventas y sus detalles de la base de datos.\n" +
            "El inventario (productos y stock) NO se modificará.\n\n" +
            "¿Confirmas el borrado completo del historial de ventas?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        // Paso 3: ejecutar
        try {
            int eliminadas = new VentaDAO().borrarTodasLasVentas();
            AlertUtil.mostrarInfo("Historial borrado", eliminadas + " venta(s) eliminadas. El inventario no fue modificado.");
        } catch (SQLException e) {
            AlertUtil.mostrarInfo("Error", "No se pudo borrar el historial: " + e.getMessage());
        }
    }

}
