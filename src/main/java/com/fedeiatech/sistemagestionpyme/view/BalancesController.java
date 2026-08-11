package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.CompraDAO;
import com.fedeiatech.sistemagestionpyme.dao.GastoDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.view.util.AlertUtil;
import java.net.URL;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 * Controller de la pestaña Balances. Carga LAZY a propósito: {@link #initialize}
 * NO consulta la base de datos — solo arma el ComboBox de período. La primera
 * query ocurre recién cuando se llama a {@link #cargar()}, disparado por fuera
 * (selección del tab), para no penalizar a un CAJERO que ni siquiera ve este tab.
 */
public class BalancesController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(BalancesController.class.getName());
    private static final String SEMANA_ACTUAL = "Semana actual";
    private static final String MES_ACTUAL = "Mes actual";
    private static final String RANGO_PERSONALIZADO = "Rango personalizado";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private AnchorPane rootPane;
    @FXML private ComboBox<String> cmbPeriodo;
    @FXML private Label lblRango;
    @FXML private Label lblVentasPeriodo;
    @FXML private Label lblComprasPeriodo;
    @FXML private Label lblGastosPeriodo;
    @FXML private Label lblBalanceNeto;
    @FXML private TableView<Venta> tblDetalle;
    @FXML private TableColumn<Venta, String> colDetalleFecha;
    @FXML private TableColumn<Venta, Integer> colDetalleId;
    @FXML private TableColumn<Venta, Double> colDetalleTotal;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final CompraDAO compraDAO = new CompraDAO();
    private final GastoDAO gastoDAO = new GastoDAO();

    private LocalDate desdeCustom;
    private LocalDate hastaCustom;
    private String periodoActivo = SEMANA_ACTUAL;
    private boolean revirtiendoComboProgramaticamente = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbPeriodo.getItems().addAll(SEMANA_ACTUAL, MES_ACTUAL, RANGO_PERSONALIZADO);
        cmbPeriodo.setValue(SEMANA_ACTUAL);
        cmbPeriodo.setOnAction(e -> onCambioPeriodo());

        colDetalleFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colDetalleId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDetalleTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
    }

    private void onCambioPeriodo() {
        if (revirtiendoComboProgramaticamente) {
            return;
        }

        if (RANGO_PERSONALIZADO.equals(cmbPeriodo.getValue())) {
            if (!pedirRangoPersonalizado()) {
                revertirComboSinRecalcular(periodoActivo);
                return;
            }
        }

        periodoActivo = cmbPeriodo.getValue();
        cargar();
    }

    /**
     * Revierte el ComboBox al período que estaba vigente antes de abrir el
     * diálogo, sin disparar un nuevo cálculo: los datos mostrados en las
     * tarjetas/tabla siguen correspondiendo a ese período previo, así que la
     * etiqueta del combo debe volver a coincidir con ellos. El flag evita que
     * el ActionEvent que dispara ComboBoxBase al reasignar {@code value}
     * reinvoque {@link #onCambioPeriodo()} y termine recalculando de más.
     */
    private void revertirComboSinRecalcular(String valorPrevio) {
        revirtiendoComboProgramaticamente = true;
        try {
            cmbPeriodo.setValue(valorPrevio);
        } finally {
            revirtiendoComboProgramaticamente = false;
        }
    }

    /** Abre el diálogo de rango personalizado (calcado de ReportsController.exportarExcel). */
    private boolean pedirRangoPersonalizado() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Rango personalizado");
        dialog.setHeaderText("Seleccioná el rango de fechas para el balance");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker pickerDesde = new DatePicker(desdeCustom != null ? desdeCustom : LocalDate.now().minusDays(30));
        DatePicker pickerHasta = new DatePicker(hastaCustom != null ? hastaCustom : LocalDate.now());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Desde:"), 0, 0);
        grid.add(pickerDesde, 1, 0);
        grid.add(new Label("Hasta:"), 0, 1);
        grid.add(pickerHasta, 1, 1);
        dialog.getDialogPane().setContent(grid);

        // No cierra el diálogo con un rango inválido: consume el ActionEvent del
        // botón OK y muestra el error hasta que el usuario corrija desde/hasta.
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(ActionEvent.ACTION, event -> {
            LocalDate desdeElegido = pickerDesde.getValue();
            LocalDate hastaElegido = pickerHasta.getValue();
            if (desdeElegido == null || hastaElegido == null || desdeElegido.isAfter(hastaElegido)) {
                AlertUtil.mostrarAdvertencia("Rango inválido",
                        "La fecha 'Desde' debe ser anterior o igual a 'Hasta'. Corregí el rango antes de continuar.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK
                || pickerDesde.getValue() == null || pickerHasta.getValue() == null) {
            return false;
        }

        desdeCustom = pickerDesde.getValue();
        hastaCustom = pickerHasta.getValue();
        return true;
    }

    /** Dispara las queries de agregación. Público para que el Dashboard lo invoque al seleccionar el tab. */
    public void cargar() {
        String periodo = cmbPeriodo.getValue();
        LocalDate hoy = LocalDate.now();
        LocalDate desde;
        LocalDate hasta;

        if (RANGO_PERSONALIZADO.equals(periodo) && desdeCustom != null && hastaCustom != null) {
            desde = desdeCustom;
            hasta = hastaCustom;
        } else if (MES_ACTUAL.equals(periodo)) {
            desde = hoy.withDayOfMonth(1);
            hasta = hoy;
        } else {
            desde = hoy.with(DayOfWeek.MONDAY);
            hasta = hoy;
        }

        lblRango.setText(desde.format(FORMATO_FECHA) + " – " + hasta.format(FORMATO_FECHA));

        try {
            double ventas = ventaDAO.sumarVentasEntre(desde.toString(), hasta.toString());
            double compras = compraDAO.sumarComprasEntre(desde.toString(), hasta.toString());
            double gastos = gastoDAO.sumarGastosEntre(desde.toString(), hasta.toString());
            double balance = ventas - compras - gastos;

            lblVentasPeriodo.setText(String.format("ARS %.2f", ventas));
            lblComprasPeriodo.setText(String.format("ARS %.2f", compras));
            lblGastosPeriodo.setText(String.format("ARS %.2f", gastos));
            lblBalanceNeto.setText(String.format("ARS %.2f", balance));
            lblBalanceNeto.setTextFill(balance >= 0 ? Color.web("#27ae60") : Color.web("#c0392b"));

            List<Venta> detalle = ventaDAO.listarVentasEntre(desde.toString(), hasta.toString());
            tblDetalle.setItems(FXCollections.observableArrayList(detalle));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar Balances", e);
            AlertUtil.mostrarError("Error al cargar Balances", "No se pudieron calcular los totales: " + e.getMessage());
        }
    }
}
