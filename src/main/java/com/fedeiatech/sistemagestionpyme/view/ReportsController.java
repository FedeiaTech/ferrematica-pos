package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.service.LicenseService;
import com.fedeiatech.sistemagestionpyme.service.TicketService;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class ReportsController implements Initializable {

    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, Integer> colId;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, Void> colAccion;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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

        Callback<TableColumn<Venta, Void>, TableCell<Venta, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Venta, Void> call(final TableColumn<Venta, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("🖨️ Ver Ticket");

                    {
                        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");

                        btn.setOnAction((ActionEvent event) -> {
                            Venta ventaSeleccionada = getTableView().getItems().get(getIndex());
                            if (ventaSeleccionada != null) {
                                reimprimirTicket(ventaSeleccionada.getId());
                            } else {
                                System.out.println("Error: No se detectó venta en la fila.");
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
        colAccion.setCellFactory(cellFactory);
    }

    private void cargarDatos() {
        VentaDAO dao = new VentaDAO();
        try {
            List<Venta> historial = dao.listarVentasHistoricas();
            tablaVentas.setItems(FXCollections.observableArrayList(historial));
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error BD", "No se pudo cargar el historial.");
        }
    }

    private void reimprimirTicket(int idVenta) {
        try {
            VentaDAO dao = new VentaDAO();
            Venta ventaCompleta = dao.obtenerVentaCompleta(idVenta);

            if (ventaCompleta == null) {
                mostrarAlerta("Error", "No se encontró la venta ID " + idVenta + " en la base de datos.");
                return;
            }

            if (ventaCompleta.getDetalles().isEmpty()) {
                mostrarAlerta("Atención", "La venta ID " + idVenta + " existe pero no tiene productos registrados (Detalles vacíos).");
            }

            TicketService ts = new TicketService();
            File ticket = ts.generarTicketPDF(ventaCompleta);

            if (ticket != null && ticket.exists()) {
                ts.abrirArchivo(ticket);
            } else {
                mostrarAlerta("Error PDF", "El archivo PDF no se pudo generar.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error Crítico", "Fallo al reimprimir: " + e.getMessage());
        }
    }

    private void mostrarBloqueo() {
        mostrarAlerta("Acceso Denegado", "No tienes licencia para ver este módulo.");
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
