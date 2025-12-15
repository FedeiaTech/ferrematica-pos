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
        // 1. Definimos la ruta simple (en la raíz de resources)
        String fxmlPath = "/dashboard_view.fxml";
        
        // 2. Comprobamos si Java realmente lo ve antes de intentar cargarlo
        if (getClass().getResource(fxmlPath) == null) {
            System.err.println("CRÍTICO: No se encuentra el archivo en: " + fxmlPath);
            System.err.println("Asegúrate de que main_view.fxml esté directamente en src/main/resources");
            return; // Detenemos para no explotar
        }

        // 3. Si existe, lo cargamos
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Sistema FedeiaTech - Pyme v1.0");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}