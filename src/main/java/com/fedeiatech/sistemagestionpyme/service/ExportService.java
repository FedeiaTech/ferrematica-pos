package com.fedeiatech.sistemagestionpyme.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExportService {

    private static final Logger LOGGER = Logger.getLogger(ExportService.class.getName());

    public File exportarResumenDiario(List<String[]> datos, File destino) throws IOException {
        String[] headers = {"Fecha", "Cant. Ventas", "Total ARS"};
        return exportar("Resumen Diario", headers, new int[]{1, 2}, datos, destino);
    }

    public File exportarPorProducto(List<String[]> datos, File destino) throws IOException {
        String[] headers = {"Producto", "Unidad", "Cantidad Vendida", "Total ARS"};
        return exportar("Por Producto", headers, new int[]{2, 3}, datos, destino);
    }

    public File exportarDetalleCompleto(List<String[]> datos, File destino) throws IOException {
        String[] headers = {"Fecha", "Nº Venta", "Producto", "Cantidad", "Unidad", "Precio Unit.", "Subtotal"};
        return exportar("Detalle Completo", headers, new int[]{1, 3, 5, 6}, datos, destino);
    }

    public File exportarGastos(List<String[]> datos, File destino) throws IOException {
        String[] headers = {"Fecha", "Concepto", "Categoría", "Monto"};
        return exportar("Gastos", headers, new int[]{3}, datos, destino);
    }

    private File exportar(String sheetName, String[] headers, int[] colsNumericas, List<String[]> datos, File destino) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);

            CellStyle numStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numStyle.setDataFormat(format.getFormat("#,##0.00"));

            CellStyle numStyleEntero = workbook.createCellStyle();
            numStyleEntero.setDataFormat(format.getFormat("#,##0"));

            Set<Integer> numCols = new HashSet<>();
            for (int c : colsNumericas) numCols.add(c);

            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < datos.size(); r++) {
                XSSFRow row = sheet.createRow(r + 1);
                String[] rowData = datos.get(r);
                for (int c = 0; c < rowData.length; c++) {
                    XSSFCell cell = row.createCell(c);
                    String val = rowData[c] != null ? rowData[c] : "";
                    if (numCols.contains(c)) {
                        try {
                            double num = Double.parseDouble(val);
                            cell.setCellValue(num);
                            cell.setCellStyle(num % 1 == 0 && c != 5 && c != 6 ? numStyleEntero : numStyle);
                        } catch (NumberFormatException e) {
                            cell.setCellValue(val);
                        }
                    } else {
                        cell.setCellValue(val);
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            if (!datos.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                workbook.write(fos);
            }
        }
        return destino;
    }

    public File generarPlantillaInventario(File destino) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Inventario");

            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);

            CellStyle exampleStyle = workbook.createCellStyle();
            XSSFFont exampleFont = workbook.createFont();
            exampleFont.setItalic(true);
            exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            exampleStyle.setFont(exampleFont);

            String[] headers = {"Código", "Nombre", "Descripción", "Precio Costo", "Precio Venta", "Stock", "Unidad", "Es Servicio"};
            String[] example = {"EJEM01", "Producto Ejemplo", "Descripción opcional", "100", "150", "10", "u", "NO"};

            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            XSSFRow exampleRow = sheet.createRow(1);
            for (int i = 0; i < example.length; i++) {
                XSSFCell cell = exampleRow.createCell(i);
                cell.setCellValue(example[i]);
                cell.setCellStyle(exampleStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                workbook.write(fos);
            }
        }
        return destino;
    }

    public void abrirArchivo(File archivo) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(archivo);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo abrir el archivo exportado", e);
        }
    }
}
