package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import com.fedeiatech.sistemagestionpyme.model.DetalleVenta;
import com.fedeiatech.sistemagestionpyme.model.Venta;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TicketService {

    private static final Rectangle TICKET_SIZE = new Rectangle(226, 800);

    public File generarTicketPDF(Venta venta) {
        File archivoDestino = null;
        try {
            Configuracion config = new ConfiguracionDAO().obtenerConfiguracion();
            String nombreEmpresa = (config != null) ? config.getNombreEmpresa() : "Mi Negocio";
            String direccion = (config != null) ? config.getDireccion() : "";

            String mensajePie = "¡Gracias por su compra!";
            if (config != null && config.getMensajeTicket() != null && !config.getMensajeTicket().trim().isEmpty()) {
                mensajePie = config.getMensajeTicket();
            }

            String rutaPersonalizada = (config != null) ? config.getRutaGuardadoTickets() : null;

            if (rutaPersonalizada != null && !rutaPersonalizada.isEmpty()) {
                File carpeta = new File(rutaPersonalizada);
                if (!carpeta.exists()) carpeta.mkdirs();

                String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String nombreArchivo = "Ticket_" + venta.getId() + "_" + fechaHora + ".pdf";

                archivoDestino = new File(carpeta, nombreArchivo);

            } else {
                archivoDestino = File.createTempFile("ticket_venta_" + venta.getId() + "_", ".pdf");
                archivoDestino.deleteOnExit();
            }

            Document document = new Document(TICKET_SIZE, 10, 10, 10, 10);
            PdfWriter.getInstance(document, new FileOutputStream(archivoDestino));

            document.open();

            if (config != null && config.getRutaLogo() != null && !config.getRutaLogo().isEmpty()) {
                try {
                    File imgFile = new File(config.getRutaLogo());
                    if (imgFile.exists()) {
                        Image img = Image.getInstance(imgFile.getAbsolutePath());
                        img.scaleToFit(100, 60);
                        img.setAlignment(Element.ALIGN_CENTER);
                        document.add(img);
                        agregarParrafo(document, " ", new Font(Font.HELVETICA, 4));
                    }
                } catch (Exception e) { }
            }

            Font fontTitulo = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font fontRegular = new Font(Font.HELVETICA, 8, Font.NORMAL);
            Font fontNegrita = new Font(Font.HELVETICA, 8, Font.BOLD);
            Font fontChica = new Font(Font.HELVETICA, 7, Font.NORMAL);

            agregarParrafo(document, nombreEmpresa, fontTitulo);
            agregarParrafo(document, direccion, fontRegular);
            if (config != null) {
                agregarParrafo(document, config.getCondicionIva(), fontChica);
                agregarParrafo(document, "CUIT: " + config.getCuit(), fontChica);
            }
            agregarParrafo(document, "--------------------------------", fontRegular);

            agregarParrafo(document, "TICKET Nro: " + String.format("%08d", venta.getId()), fontNegrita);
            agregarParrafo(document, "FECHA: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontRegular);

            agregarParrafo(document, " ", new Font(Font.HELVETICA, 2));
            Paragraph pCliente = new Paragraph("A: CONSUMIDOR FINAL", fontNegrita);
            pCliente.setAlignment(Element.ALIGN_LEFT);
            document.add(pCliente);

            Paragraph pCondIva = new Paragraph("COND. IVA: CONSUMIDOR FINAL", fontChica);
            pCondIva.setAlignment(Element.ALIGN_LEFT);
            document.add(pCondIva);

            agregarParrafo(document, "--------------------------------", fontRegular);

            DecimalFormat df = new DecimalFormat("$ #,##0.00");
            for (DetalleVenta d : venta.getDetalles()) {
                Paragraph pNombre = new Paragraph(d.getItem().getNombre(), fontRegular);
                pNombre.setAlignment(Element.ALIGN_LEFT);
                document.add(pNombre);

                String lineaNumeros = d.getCantidad() + " x " + df.format(d.getPrecioUnitario()) +
                                      " = " + df.format(d.getSubtotal());
                Paragraph pNumeros = new Paragraph(lineaNumeros, fontRegular);
                pNumeros.setAlignment(Element.ALIGN_RIGHT);
                document.add(pNumeros);
            }

            agregarParrafo(document, "--------------------------------", fontRegular);
            Paragraph pTotal = new Paragraph("TOTAL: " + df.format(venta.getTotal()), fontTitulo);
            pTotal.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(pTotal);

            agregarParrafo(document, "--------------------------------", fontRegular);
            agregarParrafo(document, mensajePie, fontRegular);

            agregarParrafo(document, ".", new Font(Font.HELVETICA, 2));
            agregarParrafo(document, "Sistema FedeiaTech", fontChica);

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return archivoDestino;
    }

    private void agregarParrafo(Document doc, String texto, Font fuente) throws Exception {
        Paragraph p = new Paragraph(texto, fuente);
        p.setAlignment(Paragraph.ALIGN_CENTER);
        doc.add(p);
    }

    public void abrirArchivo(File file) {
        try {
            if (file != null && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
