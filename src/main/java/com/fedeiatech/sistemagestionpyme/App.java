package com.fedeiatech.sistemagestionpyme;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage stage) throws IOException {
        com.fedeiatech.sistemagestionpyme.service.LoggingConfig.inicializar();
        try {
            new com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO().inicializarTabla();
            new com.fedeiatech.sistemagestionpyme.dao.UsuarioDAO().inicializarTabla();
            com.fedeiatech.sistemagestionpyme.dao.DataSeeder.sembrarDemoSiVacio();
            com.fedeiatech.sistemagestionpyme.service.SupabaseSyncService.getInstance().iniciarProgramacionSiCorresponde();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en inicialización", e);
        }
        // Generar LEEME.pdf si no existe
        try {
            java.io.File leeme = new java.io.File("LEEME.pdf");
            if (!leeme.exists()) com.fedeiatech.sistemagestionpyme.service.LeerMeService.generarLeerMe();
        } catch (Exception ignored) {}

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login_view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Sistema FedeiaTech - Pyme v1.1.0");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
