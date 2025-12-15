package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.service.LicenseService;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ReportsController implements Initializable {

    @FXML
    private TableView<Venta> tablaVentas;
    @FXML
    private TableColumn<Venta, Integer> colId;
    @FXML
    private TableColumn<Venta, String> colFecha;
    @FXML
    private TableColumn<Venta, Double> colTotal;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // DOBLE CHECK DE SEGURIDAD
        if (!LicenseService.permiteReportes()) {
            mostrarBloqueo();
            return;
        }

        configurarTabla();
        cargarDatos();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
    }

    private void cargarDatos() {
        VentaDAO dao = new VentaDAO();
        try {
            List<Venta> historial = dao.listarVentasHistoricas();
            tablaVentas.setItems(FXCollections.observableArrayList(historial));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void mostrarBloqueo() {
        // Si alguien intenta abrir esto hackeando la UI, lo cerramos
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Acceso Denegado");
        alert.setContentText("No tienes licencia para ver este módulo.");
        alert.showAndWait();
        // Cerrar ventana a la fuerza (requiere obtener stage, omitido por brevedad en este snippet)
    }
}
