package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.CompraDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.view.util.AlertUtil;
import java.net.URL;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
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
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private AnchorPane rootPane;
    @FXML private ComboBox<String> cmbPeriodo;
    @FXML private Label lblRango;
    @FXML private Label lblVentasPeriodo;
    @FXML private Label lblComprasPeriodo;
    @FXML private Label lblBalanceNeto;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final CompraDAO compraDAO = new CompraDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbPeriodo.getItems().addAll(SEMANA_ACTUAL, MES_ACTUAL);
        cmbPeriodo.setValue(SEMANA_ACTUAL);
        cmbPeriodo.setOnAction(e -> cargar());
    }

    /** Dispara las queries de agregación. Público para que el Dashboard lo invoque al seleccionar el tab. */
    public void cargar() {
        boolean esSemana = SEMANA_ACTUAL.equals(cmbPeriodo.getValue());
        LocalDate hoy = LocalDate.now();
        LocalDate desde = esSemana ? hoy.with(DayOfWeek.MONDAY) : hoy.withDayOfMonth(1);
        LocalDate hasta = hoy;

        lblRango.setText(desde.format(FORMATO_FECHA) + " – " + hasta.format(FORMATO_FECHA));

        try {
            double ventas = ventaDAO.sumarVentasEntre(desde.toString(), hasta.toString());
            double compras = compraDAO.sumarComprasEntre(desde.toString(), hasta.toString());
            double balance = ventas - compras;

            lblVentasPeriodo.setText(String.format("ARS %.2f", ventas));
            lblComprasPeriodo.setText(String.format("ARS %.2f", compras));
            lblBalanceNeto.setText(String.format("ARS %.2f", balance));
            lblBalanceNeto.setTextFill(balance >= 0 ? Color.web("#27ae60") : Color.web("#c0392b"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar Balances", e);
            AlertUtil.mostrarError("Error al cargar Balances", "No se pudieron calcular los totales: " + e.getMessage());
        }
    }
}
