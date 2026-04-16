package com.fedeiatech.sistemagestionpyme;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        try {
            new com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO().inicializarTabla();
            new com.fedeiatech.sistemagestionpyme.dao.UsuarioDAO().inicializarTabla();
            com.fedeiatech.sistemagestionpyme.dao.DataSeeder.sembrarDemoSiVacio();
        } catch (Exception e) {
            System.err.println("Error en inicialización: " + e.getMessage());
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login_view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Sistema FedeiaTech - Pyme v0.7");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
