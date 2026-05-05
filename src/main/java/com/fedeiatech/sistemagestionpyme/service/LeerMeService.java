package com.fedeiatech.sistemagestionpyme.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;

public class LeerMeService {

    private static final String VERSION = "v0.8.0";
    private static final String AUTOR   = "Federico Iacono — IATech";
    private static final String DERECHOS = "© 2026 IATech. Todos los derechos reservados.";

    public static File generarLeerMe() throws Exception {
        File archivo = new File("LEEME.pdf");
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(archivo));
        doc.open();

        PdfContentByte canvas = writer.getDirectContentUnder();
        canvas.setColorFill(Color.BLACK);
        canvas.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
        canvas.fill();

        Font fTitulo  = new Font(Font.HELVETICA, 22, Font.BOLD,  Color.WHITE);
        Font fSub     = new Font(Font.HELVETICA, 13, Font.BOLD,  new Color(100, 200, 255));
        Font fNormal  = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.WHITE);
        Font fGris    = new Font(Font.HELVETICA, 9,  Font.NORMAL, new Color(160, 160, 160));
        Font fAccent  = new Font(Font.HELVETICA, 10, Font.BOLD,  new Color(100, 200, 255));
        Font fVerde   = new Font(Font.HELVETICA, 10, Font.BOLD,  new Color(80, 210, 120));
        Font fAmarillo= new Font(Font.HELVETICA, 10, Font.BOLD,  new Color(255, 200, 50));

        // Encabezado
        agregarP(doc, "Sistema de Gestión PyME", fTitulo, Element.ALIGN_CENTER);
        agregarP(doc, AUTOR + "  |  " + VERSION, fGris, Element.ALIGN_CENTER);
        agregarP(doc, DERECHOS, fGris, Element.ALIGN_CENTER);
        agregarLinea(doc);

        // Primer uso
        agregarP(doc, "PRIMER USO", fSub, Element.ALIGN_LEFT);
        agregarP(doc, "Al iniciar por primera vez, el sistema crea automáticamente un usuario administrador:", fNormal, Element.ALIGN_LEFT);
        agregarP(doc, "   Usuario: admin     Contraseña: admin", fVerde, Element.ALIGN_LEFT);
        agregarP(doc, "Se recomienda cambiar la contraseña desde el botón Usuarios en el dashboard antes de operar.", fGris, Element.ALIGN_LEFT);
        agregarLinea(doc);

        // Premium
        agregarP(doc, "ACTIVACIÓN PREMIUM", fSub, Element.ALIGN_LEFT);
        agregarP(doc, "El sistema funciona en modo Free desde el primer arranque. Para desbloquear todas las funciones:", fNormal, Element.ALIGN_LEFT);
        agregarP(doc, "   Dashboard → botón PREMIUM → ingresar la clave de activación provista por IATech.", fAccent, Element.ALIGN_LEFT);
        agregarP(doc, "El desbloqueo es permanente y queda guardado en la base de datos local.", fGris, Element.ALIGN_LEFT);
        agregarLinea(doc);

        // Módulos
        agregarP(doc, "MÓDULOS PRINCIPALES", fSub, Element.ALIGN_LEFT);
        modulo(doc, "Punto de Venta (POS)", "Buscá productos por código o nombre. Seleccioná y cobrá. El stock se descuenta automáticamente al confirmar la venta. El ticket PDF se genera al instante.", fNormal, fAccent);
        modulo(doc, "Inventario", "Administrá productos, precios, stock y unidades (unidad, kg, g, lt). Creá combos de productos con stock calculado automáticamente.", fNormal, fAccent);
        modulo(doc, "Reportes (PREMIUM)", "Historial completo de ventas, reimpresión de tickets y exportación a Excel en tres formatos.", fNormal, fAccent);
        modulo(doc, "Estadísticas (PREMIUM)", "Canasta de productos frecuentes, mejor horario de venta, mapa de demanda y tendencias semanales/mensuales.", fNormal, fAccent);
        modulo(doc, "Configuración", "Datos de empresa, logo, formato del ticket (58mm/80mm), margen de ganancia, backup y más.", fNormal, fAccent);
        agregarLinea(doc);

        // Base de datos
        agregarP(doc, "BASE DE DATOS", fSub, Element.ALIGN_LEFT);
        agregarP(doc, "El archivo gestion_pyme.db contiene TODOS los datos (ventas, productos, configuración).", fNormal, Element.ALIGN_LEFT);
        agregarP(doc, "Ubicación: carpeta donde está instalado el programa.", fAmarillo, Element.ALIGN_LEFT);
        agregarP(doc, "Hacé backup periódico desde Configuración → Sistema y Seguridad → Generar Backup.", fNormal, Element.ALIGN_LEFT);
        agregarLinea(doc);

        // Tickets PDF
        agregarP(doc, "TICKETS PDF", fSub, Element.ALIGN_LEFT);
        agregarP(doc, "Si no configurás una carpeta de destino, los tickets se guardan en la carpeta temporal del sistema.", fNormal, Element.ALIGN_LEFT);
        agregarP(doc, "Los datos de ventas siempre están en la DB aunque no haya PDF guardado.", fGris, Element.ALIGN_LEFT);
        agregarLinea(doc);

        // Contacto
        agregarP(doc, "SOPORTE Y CONTACTO", fSub, Element.ALIGN_LEFT);
        agregarP(doc, "IATech  —  Federico Iacono", fNormal, Element.ALIGN_LEFT);
        agregarP(doc, "iaconofede@gmail.com", fAccent, Element.ALIGN_LEFT);
        agregarP(doc, "Instagram: iatech.dev", fAccent, Element.ALIGN_LEFT);

        doc.close();
        return archivo;
    }

    private static void modulo(Document doc, String titulo, String desc, Font fNormal, Font fTit) throws Exception {
        Paragraph p = new Paragraph();
        p.add(new Chunk("• " + titulo + ": ", fTit));
        p.add(new Chunk(desc, fNormal));
        p.setSpacingBefore(4);
        doc.add(p);
    }

    private static void agregarP(Document doc, String texto, Font font, int align) throws Exception {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(align);
        p.setSpacingBefore(4);
        doc.add(p);
    }

    private static void agregarLinea(Document doc) throws Exception {
        Paragraph sep = new Paragraph("─────────────────────────────────────────────────────",
            new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(60, 60, 60)));
        sep.setSpacingBefore(6);
        sep.setSpacingAfter(6);
        doc.add(sep);
    }

    public static void abrirLeerMe() {
        try {
            File f = new File("LEEME.pdf");
            if (!f.exists()) f = generarLeerMe();
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
