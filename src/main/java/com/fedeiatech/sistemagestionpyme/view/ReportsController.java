package com.fedeiatech.sistemagestionpyme.view;

import com.fedeiatech.sistemagestionpyme.dao.UsuarioDAO;
import com.fedeiatech.sistemagestionpyme.dao.VentaDAO;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.Usuario;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.fedeiatech.sistemagestionpyme.service.ExportService;
import com.fedeiatech.sistemagestionpyme.service.SessionService;
import com.fedeiatech.sistemagestionpyme.service.SupabaseSyncService;
import com.fedeiatech.sistemagestionpyme.service.ThemeService;
import com.fedeiatech.sistemagestionpyme.service.TicketService;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import com.fedeiatech.sistemagestionpyme.view.util.AlertUtil;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ReportsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ReportsController.class.getName());

    @FXML private AnchorPane rootPane;
    @FXML private TableView<Object> tablaVentas;
    @FXML private TableColumn<Object, String> colId;
    @FXML private TableColumn<Object, String> colFecha;
    @FXML private TableColumn<Object, String> colTotal;
    @FXML private TableColumn<Object, String> colEstado;
    @FXML private TableColumn<Object, String> colEstadoEnvio;
    @FXML private TableColumn<Object, String> colHoraEntrega;
    @FXML private TableColumn<Object, Void> colAccion;
    @FXML private Button btnExportar;
    @FXML private Label lblAvisoEnvios;

    // Pastel por estado — ver spec pos-reportes-estado-envio. Status sin match (o no-pedido) queda sin color.
    private static final Map<String, String> COLORES_ESTADO_ENVIO = Map.of(
        "entregado", "#d4f4dd",
        "en_camino", "#fff3cd",
        "asignado", "#d6e9f8",
        "pendiente", "#e8e8e8",
        "cancelado", "#f8d7da");

    static String colorEstadoEnvio(String status) {
        return status == null ? null : COLORES_ESTADO_ENVIO.get(status);
    }

    /** Estado del PDF de ticket para una venta, ver {@link #reimprimirTicket(Venta)}. */
    enum EstadoTicket { NUNCA_GENERADO, EXISTE, FALTA }

    static EstadoTicket estadoTicket(String rutaTicket, boolean archivoExiste) {
        if (rutaTicket == null || rutaTicket.isBlank()) return EstadoTicket.NUNCA_GENERADO;
        return archivoExiste ? EstadoTicket.EXISTE : EstadoTicket.FALTA;
    }

    static String textoBotonTicket(EstadoTicket estado) {
        return switch (estado) {
            case NUNCA_GENERADO -> "🖨️ Ver Ticket";
            case EXISTE -> "✅ Ver Ticket";
            case FALTA -> "⚠️ Ver Ticket";
        };
    }

    static String estiloBotonTicket(EstadoTicket estado) {
        return switch (estado) {
            case NUNCA_GENERADO -> "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;";
            case EXISTE -> "-fx-background-color: #d4f4dd; -fx-text-fill: #1e7e34; -fx-font-size: 11px; -fx-cursor: hand;";
            case FALTA -> "-fx-background-color: #f8d7da; -fx-text-fill: #a94442; -fx-font-size: 11px; -fx-cursor: hand;";
        };
    }

    /**
     * Segunda línea, subordinada, de la celda de estado de envío (design decision DA8). {@code null}
     * cuando no hay saldo pendiente que mostrar — línea 2 debe quedar colapsada, no en blanco.
     */
    static String formatearSaldoPendiente(Double saldoPendiente) {
        return saldoPendiente == null ? null : String.format("falta $ %.2f", saldoPendiente);
    }

    /** Segunda línea del tooltip (design decision DA8). {@code null} cuando no hay saldo pendiente. */
    static String formatearTooltipSaldoPendiente(Double saldoPendiente) {
        return saldoPendiente == null ? null : String.format("Saldo pendiente: $ %.2f", saldoPendiente);
    }

    private static final DateTimeFormatter FORMATO_HORA_ENTREGA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * "Hora de entrega" solo tiene sentido para un pedido ya entregado (design decision, mismo
     * criterio que {@link #formatearSaldoPendiente}: {@code null} es un estado válido, no un fallo).
     * Un {@code status == "entregado"} sin {@code entregadoEn} (backend pre-0014, o el RPC nunca
     * llegó a persistir el timestamp) también degrada a guion, no a excepción.
     */
    static String formatearHoraEntrega(String status, Instant entregadoEn) {
        if (!"entregado".equals(status)) return "—";
        if (entregadoEn == null) return "Entregado (hora desconocida)";
        return FORMATO_HORA_ENTREGA.format(entregadoEn.atZone(ZoneId.systemDefault()));
    }

    static String formatearEstadoEnvio(String status) {
        if (status == null || status.isBlank()) return "—";
        String[] palabras = status.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private Map<Integer, SupabaseSyncService.EstadoEnvio> estadoEnvios = Map.of();

    /** Fila sintética inyectada en la tabla para mostrar el detalle de un ticket expandido. */
    private static class FilaDetalleTicket {
        final DetalleVenta detalle;
        FilaDetalleTicket(DetalleVenta detalle) { this.detalle = detalle; }
    }

    private final ObservableList<Object> filasVisibles = FXCollections.observableArrayList();
    private Venta ventaExpandida = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rootPane.setStyle(ThemeService.getInstance().getBgStyle());
        rootPane.sceneProperty().addListener((obs, sceneAnterior, sceneNueva) -> {
            if (sceneNueva != null) {
                sceneNueva.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        ((Stage) sceneNueva.getWindow()).close();
                    }
                });
            }
        });

        configurarTabla();
        cargarDatos();
        cargarEstadoEnvios();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue() instanceof Venta v ? String.valueOf(v.getId()) : "↳"));

        colFecha.setCellValueFactory(data -> {
            Object o = data.getValue();
            if (o instanceof Venta v) return new javafx.beans.property.SimpleStringProperty(v.getFecha());
            DetalleVenta d = ((FilaDetalleTicket) o).detalle;
            String unidad = d.esCombo() ? "u" : d.getItem().getUnidad();
            return new javafx.beans.property.SimpleStringProperty(
                "      • " + d.getNombreItem() + "  (" + formatearCantidad(d.getCantidad()) + " " + unidad + ")");
        });

        colTotal.setCellValueFactory(data -> {
            Object o = data.getValue();
            if (o instanceof Venta v) return new javafx.beans.property.SimpleStringProperty(String.format("%.2f", v.getTotal()));
            DetalleVenta d = ((FilaDetalleTicket) o).detalle;
            return new javafx.beans.property.SimpleStringProperty(String.format("$ %.2f", d.getSubtotal()));
        });

        colEstado.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue() instanceof Venta v ? v.getEstado() : ""));

        colEstadoEnvio.setCellValueFactory(data -> {
            SupabaseSyncService.EstadoEnvio estado = data.getValue() instanceof Venta v
                ? estadoEnvios.get(v.getId()) : null;
            return new javafx.beans.property.SimpleStringProperty(estado != null ? estado.status() : null);
        });
        colEstadoEnvio.setCellFactory(col -> new TableCell<>() {
            // Nodos reutilizados en cada updateItem (mismo patrón que la columna de acciones,
            // ver más abajo) — evita reasignar el grafo de nodos en cada frame de scroll.
            private final Label lblEstado = new Label();
            private final Label lblSaldo = new Label();
            private final VBox caja = new VBox(1, lblEstado, lblSaldo);
            {
                caja.setAlignment(Pos.CENTER);
                lblSaldo.setStyle("-fx-font-size: 10px; -fx-opacity: 0.75;");
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                // Esta columna solo tiene sentido en filas de Venta — las filas sintéticas de
                // detalle de ticket (insertadas al expandir, ver FilaDetalleTicket) no tienen
                // estado de envío propio y deben quedar completamente en blanco, sin guion.
                boolean esFilaVenta = getTableRow() != null && getTableRow().getItem() instanceof Venta;
                if (empty || !esFilaVenta) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setTooltip(null);
                    return;
                }
                String color = colorEstadoEnvio(status);
                if (color == null) {
                    setText("—");
                    setGraphic(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(null);
                    lblEstado.setText(formatearEstadoEnvio(status));
                    String textoSaldo = formatearSaldoPendiente(saldoPendienteParaFila());
                    boolean tieneSaldo = textoSaldo != null;
                    lblSaldo.setText(tieneSaldo ? textoSaldo : "");
                    lblSaldo.setVisible(tieneSaldo);
                    lblSaldo.setManaged(tieneSaldo);
                    setGraphic(caja);
                    setStyle("-fx-background-color: " + color + ";");
                    setTooltip(tooltipParaFila());
                }
            }

            private Double saldoPendienteParaFila() {
                if (!(getTableRow() != null && getTableRow().getItem() instanceof Venta v)) return null;
                SupabaseSyncService.EstadoEnvio estado = estadoEnvios.get(v.getId());
                return estado != null ? estado.saldoPendiente() : null;
            }

            private javafx.scene.control.Tooltip tooltipParaFila() {
                if (!(getTableRow() != null && getTableRow().getItem() instanceof Venta v)) return null;
                SupabaseSyncService.EstadoEnvio estado = estadoEnvios.get(v.getId());
                if (estado == null) return null;
                StringBuilder sb = new StringBuilder();
                if (estado.actualizadoEn() != null) {
                    sb.append("Actualizado: ").append(estado.actualizadoEn());
                }
                String lineaSaldo = formatearTooltipSaldoPendiente(estado.saldoPendiente());
                if (lineaSaldo != null) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(lineaSaldo);
                }
                return sb.length() > 0 ? new javafx.scene.control.Tooltip(sb.toString()) : null;
            }
        });

        colHoraEntrega.setCellValueFactory(data -> {
            if (!(data.getValue() instanceof Venta v)) return new javafx.beans.property.SimpleStringProperty(null);
            SupabaseSyncService.EstadoEnvio estado = estadoEnvios.get(v.getId());
            String status = estado != null ? estado.status() : null;
            Instant entregadoEn = estado != null ? estado.entregadoEn() : null;
            return new javafx.beans.property.SimpleStringProperty(formatearHoraEntrega(status, entregadoEn));
        });
        colHoraEntrega.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean empty) {
                super.updateItem(texto, empty);
                boolean esFilaVenta = getTableRow() != null && getTableRow().getItem() instanceof Venta;
                setText(empty || !esFilaVenta ? null : texto);
            }
        });

        boolean esAdmin = SessionService.getInstance().esAdmin();

        Callback<TableColumn<Object, Void>, TableCell<Object, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Object, Void> call(final TableColumn<Object, Void> param) {
                return new TableCell<>() {
                    private final Button btnVer = new Button("🖨️ Ver Ticket");
                    private final Button btnAnular = new Button("🚫 Anular");
                    private final Button btnBorrar = new Button("🗑️");
                    private final HBox contenedor = new HBox(6, btnVer, btnAnular, btnBorrar);

                    {
                        btnVer.setOnAction((ActionEvent event) -> {
                            Venta ventaSeleccionada = (Venta) getTableView().getItems().get(getIndex());
                            reimprimirTicket(ventaSeleccionada);
                        });
                        btnAnular.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                        btnAnular.setVisible(esAdmin);
                        btnAnular.setManaged(esAdmin);
                        btnAnular.setOnAction((ActionEvent event) -> {
                            Venta ventaSeleccionada = (Venta) getTableView().getItems().get(getIndex());
                            anularVenta(ventaSeleccionada);
                        });
                        btnBorrar.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                        btnBorrar.setVisible(esAdmin);
                        btnBorrar.setManaged(esAdmin);
                        btnBorrar.setOnAction((ActionEvent event) -> {
                            Venta ventaSeleccionada = (Venta) getTableView().getItems().get(getIndex());
                            borrarTicket(ventaSeleccionada);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || !(getTableView().getItems().get(getIndex()) instanceof Venta venta)) {
                            setGraphic(null);
                        } else {
                            btnAnular.setDisable(venta.estaAnulada());
                            actualizarEstadoBotonTicket(venta);
                            setGraphic(contenedor);
                        }
                    }

                    /**
                     * Refleja en el botón si el ticket nunca se generó, si el PDF sigue en la
                     * carpeta configurada, o si la ruta guardada ya no apunta a un archivo real
                     * (mismos colores pastel que la columna de estado de envío, ver
                     * COLORES_ESTADO_ENVIO). El chequeo de disco solo corre para filas visibles
                     * — TableView virtualiza las celdas, así que no escanea toda la tabla.
                     */
                    private void actualizarEstadoBotonTicket(Venta venta) {
                        String ruta = venta.getRutaTicket();
                        boolean existe = ruta != null && !ruta.isBlank() && new File(ruta).exists();
                        EstadoTicket estado = estadoTicket(ruta, existe);
                        btnVer.setText(textoBotonTicket(estado));
                        btnVer.setStyle(estiloBotonTicket(estado));
                        btnVer.setTooltip(switch (estado) {
                            case NUNCA_GENERADO -> null;
                            case EXISTE -> new javafx.scene.control.Tooltip(ruta);
                            case FALTA -> new javafx.scene.control.Tooltip("No se encuentra en: " + ruta);
                        });
                    }
                };
            }
        };
        colAccion.setCellFactory(cellFactory);

        tablaVentas.setRowFactory(tv -> {
            TableRow<Object> fila = new TableRow<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    setStyle(!empty && item instanceof FilaDetalleTicket
                        ? "-fx-background-color: #f4f6f8; -fx-font-style: italic;" : "");
                }
            };
            fila.setOnMouseClicked(event -> {
                if (fila.isEmpty() || esClickEnBoton(event, fila)) return;
                if (fila.getItem() instanceof Venta venta) toggleExpandir(venta);
            });
            return fila;
        });
    }

    private boolean esClickEnBoton(javafx.scene.input.MouseEvent event, TableRow<Object> fila) {
        javafx.scene.Node nodo = event.getTarget() instanceof javafx.scene.Node n ? n : null;
        while (nodo != null && nodo != fila) {
            if (nodo instanceof Button) return true;
            nodo = nodo.getParent();
        }
        return false;
    }

    private void toggleExpandir(Venta venta) {
        boolean yaExpandida = venta == ventaExpandida;
        colapsarDetalle();
        if (yaExpandida) return;

        try {
            Venta completa = new VentaDAO().obtenerVentaCompleta(venta.getId());
            if (completa == null || completa.getDetalles().isEmpty()) {
                AlertUtil.mostrarAdvertencia("Sin detalle", "No se encontró el detalle del ticket #" + venta.getId());
                return;
            }
            int idx = filasVisibles.indexOf(venta);
            if (idx < 0) return;

            List<Object> filasDetalle = new java.util.ArrayList<>();
            for (DetalleVenta d : completa.getDetalles()) filasDetalle.add(new FilaDetalleTicket(d));
            filasVisibles.addAll(idx + 1, filasDetalle);
            ventaExpandida = venta;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar detalle del ticket " + venta.getId(), e);
            AlertUtil.mostrarAdvertencia("Error DB", "No se pudo cargar el detalle del ticket: " + e.getMessage());
        }
    }

    private void colapsarDetalle() {
        if (ventaExpandida == null) return;
        filasVisibles.removeIf(f -> f instanceof FilaDetalleTicket);
        ventaExpandida = null;
    }

    private String formatearCantidad(double valor) {
        return valor % 1 == 0 ? String.valueOf((int) valor) : String.valueOf(valor);
    }

    private void cargarDatos() {
        VentaDAO dao = new VentaDAO();
        try {
            List<Venta> historial = dao.listarVentasHistoricas();
            ventaExpandida = null;
            filasVisibles.setAll(historial);
            if (tablaVentas.getItems() != filasVisibles) tablaVentas.setItems(filasVisibles);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el historial de ventas", e);
            AlertUtil.mostrarAdvertencia("Error BD", "No se pudo cargar el historial.");
        }
    }

    /**
     * Consulta el estado de envío en un hilo aparte (mismo patrón que
     * {@code DashboardController#sincronizarManualDesdeIndicador}) — nunca bloquea la apertura de
     * Reportes. Ante cualquier fallo, {@link SupabaseSyncService#obtenerEstadoEnvios()} devuelve
     * {@code exitoso=false} y la columna queda en blanco para todas las filas mostrando el aviso no
     * bloqueante, sin diálogo de error (spec pos-reportes-estado-envio). Una consulta exitosa con
     * cero envíos vinculados NO dispara el aviso — son estados distintos, no ambos "sin datos".
     */
    private void cargarEstadoEnvios() {
        new Thread(() -> {
            SupabaseSyncService.ResultadoEstadoEnvios resultado =
                SupabaseSyncService.getInstance().obtenerEstadoEnvios();
            javafx.application.Platform.runLater(() -> {
                estadoEnvios = resultado.estados();
                tablaVentas.refresh();
                if (lblAvisoEnvios != null) {
                    lblAvisoEnvios.setVisible(!resultado.exitoso());
                    lblAvisoEnvios.setManaged(!resultado.exitoso());
                }
            });
        }, "reportes-estado-envios").start();
    }

    @FXML
    void exportarExcel(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Exportar a Excel");
        dialog.setHeaderText("Seleccioná el período y tipo de reporte");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker pickerDesde = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker pickerHasta = new DatePicker(LocalDate.now());
        ChoiceBox<String> tipoReporte = new ChoiceBox<>();
        tipoReporte.getItems().addAll("Resumen diario", "Por producto", "Detalle completo");
        tipoReporte.setValue("Resumen diario");
        tipoReporte.setPrefWidth(200);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Desde:"), 0, 0);
        grid.add(pickerDesde, 1, 0);
        grid.add(new Label("Hasta:"), 0, 1);
        grid.add(pickerHasta, 1, 1);
        grid.add(new Label("Tipo:"), 0, 2);
        grid.add(tipoReporte, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String desde = pickerDesde.getValue().toString();
        String hasta = pickerHasta.getValue().toString();
        String tipo = tipoReporte.getValue();

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("ventas_" + tipo.replace(" ", "_").toLowerCase() + "_" + desde + ".xlsx");
        String docPath = System.getProperty("user.home") + "/Documents";
        File docDir = new File(docPath);
        if (docDir.exists()) fc.setInitialDirectory(docDir);
        File archivo = fc.showSaveDialog(btnExportar.getScene().getWindow());
        if (archivo == null) return;

        try {
            VentaDAO dao = new VentaDAO();
            ExportService exportService = new ExportService();
            List<String[]> datos;
            File generado;

            switch (tipo) {
                case "Por producto" -> {
                    datos = dao.obtenerVentasPorProducto(desde, hasta);
                    generado = exportService.exportarPorProducto(datos, archivo);
                }
                case "Detalle completo" -> {
                    datos = dao.obtenerDetalleCompleto(desde, hasta);
                    generado = exportService.exportarDetalleCompleto(datos, archivo);
                }
                default -> {
                    datos = dao.obtenerResumenDiario(desde, hasta);
                    generado = exportService.exportarResumenDiario(datos, archivo);
                }
            }

            exportService.abrirArchivo(generado);
            AlertUtil.mostrarInfo("Exportación exitosa",
                    datos.size() + " filas exportadas a:\n" + generado.getName());

        } catch (Exception e) {
            AlertUtil.mostrarAdvertencia("Error al exportar", e.getMessage());
        }
    }

    /**
     * Reautentica al usuario admin activo pidiéndole la contraseña por diálogo.
     * Devuelve true solo si el diálogo fue confirmado y la contraseña es correcta.
     * Muestra los mensajes de error/cancelación correspondientes por sí misma.
     */
    private boolean reautenticarAdmin(String motivoAccion) {
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("Contraseña del administrador");
        Dialog<ButtonType> dlgPass = new Dialog<>();
        dlgPass.setTitle("Confirmar identidad");
        dlgPass.setHeaderText(motivoAccion);
        dlgPass.getDialogPane().setContent(pfPass);
        dlgPass.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dlgPass.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return false;

        try {
            String nombreAdmin = SessionService.getInstance().getUsuarioActivo().getNombre();
            Usuario verificado = new UsuarioDAO().autenticar(nombreAdmin, pfPass.getText());
            if (verificado == null) {
                AlertUtil.mostrarInfo("Contraseña incorrecta", "La contraseña ingresada no es válida.");
                return false;
            }
            return true;
        } catch (Exception e) {
            AlertUtil.mostrarInfo("Error", "No se pudo verificar la identidad: " + e.getMessage());
            return false;
        }
    }

    private void borrarTicket(Venta venta) {
        if (!reautenticarAdmin("Ingresá la contraseña del administrador para continuar.")) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Borrar ticket");
        confirm.setHeaderText("Esta acción es IRREVERSIBLE.");
        confirm.setContentText(
            "Se eliminará el ticket #" + venta.getId() + " (" + venta.getFecha() + ", ARS " + venta.getTotal() + ") y sus detalles.\n" +
            "El inventario (productos y stock) NO se modificará.\n\n" +
            "¿Confirmás el borrado de este ticket?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            boolean eliminado = new VentaDAO().borrarVenta(venta.getId());
            if (eliminado) {
                tablaVentas.getItems().remove(venta);
                AlertUtil.mostrarInfo("Ticket borrado", "El ticket #" + venta.getId() + " fue eliminado. El inventario no fue modificado.");
            } else {
                AlertUtil.mostrarAdvertencia("No encontrado", "El ticket #" + venta.getId() + " ya no existe.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al borrar el ticket " + venta.getId(), e);
            AlertUtil.mostrarInfo("Error", "No se pudo borrar el ticket: " + e.getMessage());
        }
    }

    private void anularVenta(Venta venta) {
        if (venta.estaAnulada()) {
            AlertUtil.mostrarInfo("Ya anulada", "El ticket #" + venta.getId() + " ya estaba anulado.");
            return;
        }

        TextField tfMotivo = new TextField();
        tfMotivo.setPromptText("Motivo de la anulación");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Motivo:"), 0, 0);
        grid.add(tfMotivo, 1, 0);

        Dialog<ButtonType> dlgMotivo = new Dialog<>();
        dlgMotivo.setTitle("Motivo de la anulación");
        dlgMotivo.setHeaderText("Ingresá el motivo de la anulación del ticket #" + venta.getId() + ".");
        dlgMotivo.getDialogPane().setContent(grid);
        dlgMotivo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dlgMotivo.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        if (!reautenticarAdmin("Ingresá la contraseña del administrador para anular el ticket #" + venta.getId() + ".")) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Anular venta");
        confirm.setHeaderText("Esta acción repone el stock vendido.");
        confirm.setContentText(
            "Se anulará el ticket #" + venta.getId() + " (" + venta.getFecha() + ", ARS " + venta.getTotal() + ").\n" +
            "El stock de los productos vendidos se repondrá automáticamente.\n\n" +
            "¿Confirmás la anulación de este ticket?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            boolean anulada = new VentaDAO().anularVenta(venta.getId(), tfMotivo.getText());
            if (anulada) {
                venta.setEstado(Venta.ESTADO_ANULADA);
                venta.setMotivoAnulacion(tfMotivo.getText());
                tablaVentas.refresh();
                AlertUtil.mostrarInfo("Venta anulada", "El ticket #" + venta.getId() + " fue anulado y su stock repuesto.");
            } else {
                AlertUtil.mostrarAdvertencia("No se pudo anular", "El ticket #" + venta.getId() + " ya estaba anulado o no existe.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al anular el ticket " + venta.getId(), e);
            AlertUtil.mostrarInfo("Error", "No se pudo anular el ticket: " + e.getMessage());
        }
    }

    /**
     * Ver/reimprimir ticket. Si ya existe una ruta guardada y el archivo sigue ahí, lo abre
     * directo sin regenerar. Si nunca se generó, genera y persiste la ruta. Si había una ruta
     * pero el archivo ya no está (carpeta movida, borrado a mano), confirma con el usuario antes
     * de regenerar — no lo hace en silencio.
     */
    private void reimprimirTicket(Venta venta) {
        String rutaActual = venta.getRutaTicket();
        if (rutaActual != null && !rutaActual.isBlank()) {
            File existente = new File(rutaActual);
            if (existente.exists()) {
                new TicketService().abrirArchivo(existente);
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Ticket no encontrado");
            confirm.setHeaderText("El ticket no se encuentra en la carpeta configurada.");
            confirm.setContentText("¿Generarlo de nuevo?");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        }
        generarYAbrirTicket(venta);
    }

    private void generarYAbrirTicket(Venta venta) {
        try {
            VentaDAO dao = new VentaDAO();
            Venta ventaCompleta = dao.obtenerVentaCompleta(venta.getId());
            if (ventaCompleta == null) {
                AlertUtil.mostrarAdvertencia("Error", "No se encontró la venta ID " + venta.getId());
                return;
            }
            TicketService ts = new TicketService();
            File ticket = ts.generarTicketPDF(ventaCompleta);
            if (ticket != null && ticket.exists()) {
                dao.actualizarRutaTicket(venta.getId(), ticket.getAbsolutePath());
                venta.setRutaTicket(ticket.getAbsolutePath());
                tablaVentas.refresh();
                ts.abrirArchivo(ticket);
            } else {
                AlertUtil.mostrarAdvertencia("Error PDF", "El archivo PDF no se pudo generar.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar/reimprimir ticket", e);
            AlertUtil.mostrarAdvertencia("Error Crítico", "Fallo al generar el ticket: " + e.getMessage());
        }
    }

    @FXML
    void abrirEstadisticas(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/stats_view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Estadísticas Avanzadas");
            stage.setScene(new Scene(root));
            stage.initOwner(rootPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (Exception e) {
            AlertUtil.mostrarAdvertencia("Error", "No se pudo abrir estadísticas: " + e.getMessage());
        }
    }

}
