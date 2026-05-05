package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ImportService {

    private static final Set<String> UNIDADES_VALIDAS = Set.of("u", "kg", "g", "lt");

    public static class ImportResult {
        public final List<ItemVenta> validos = new ArrayList<>();
        public final List<String> errores = new ArrayList<>();
    }

    private record ParseResult(double valor, String error) {}

    public ImportResult importarDesdeExcel(File archivo) throws IOException {
        ImportResult result = new ImportResult();

        try (FileInputStream fis = new FileInputStream(archivo);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row)) continue;
                procesarFila(row, i + 1, result);
            }
        }

        return result;
    }

    private void procesarFila(Row row, int numFila, ImportResult result) {
        List<String> erroresFila = new ArrayList<>();

        String codigo = leerString(row, 0);
        if (esErrorCelda(codigo)) erroresFila.add("Código con error de celda/fórmula");
        else if (codigo.isEmpty()) erroresFila.add("Código vacío");

        String nombre = leerString(row, 1);
        if (esErrorCelda(nombre)) erroresFila.add("Nombre con error de celda/fórmula");
        else if (nombre.isEmpty()) erroresFila.add("Nombre vacío");

        String descripcion = leerString(row, 2);
        if (esErrorCelda(descripcion)) descripcion = "";

        double precioCosto = 0.0;
        String rawCosto = leerString(row, 3);
        if (!rawCosto.isEmpty()) {
            ParseResult pr = parsearNumero(rawCosto, "Precio Costo");
            if (pr.error() != null) erroresFila.add(pr.error());
            else if (pr.valor() < 0) erroresFila.add("Precio Costo negativo (" + rawCosto + ")");
            else precioCosto = pr.valor();
        }

        double precioVenta = 0.0;
        String rawVenta = leerString(row, 4);
        if (rawVenta.isEmpty()) {
            erroresFila.add("Precio Venta vacío");
        } else {
            ParseResult pr = parsearNumero(rawVenta, "Precio Venta");
            if (pr.error() != null) erroresFila.add(pr.error());
            else if (pr.valor() < 0) erroresFila.add("Precio Venta negativo (" + rawVenta + ")");
            else precioVenta = pr.valor();
        }

        boolean esServicio = parsearEsServicio(leerString(row, 7));

        double stock = 0.0;
        if (!esServicio) {
            String rawStock = leerString(row, 5);
            if (!rawStock.isEmpty()) {
                ParseResult pr = parsearNumero(rawStock, "Stock");
                if (pr.error() != null) erroresFila.add(pr.error());
                else if (pr.valor() < 0) erroresFila.add("Stock negativo (" + rawStock + ")");
                else stock = pr.valor();
            }
        } else {
            stock = -1;
        }

        String unidad = leerString(row, 6).toLowerCase().trim();
        if (esErrorCelda(unidad) || unidad.isEmpty()) {
            unidad = "u";
        } else if (!UNIDADES_VALIDAS.contains(unidad)) {
            erroresFila.add("Unidad inválida ('" + unidad + "'). Válidas: u / kg / g / lt");
        }

        if (!erroresFila.isEmpty()) {
            result.errores.add("Fila " + numFila + ": " + String.join("; ", erroresFila));
            return;
        }

        ItemVenta item = new ItemVenta();
        item.setCodigo(codigo);
        item.setNombre(nombre);
        item.setDescripcion(descripcion);
        item.setPrecioCosto(precioCosto);
        item.setPrecioVenta(precioVenta);
        item.setStock(stock);
        item.setEsServicio(esServicio);
        item.setUnidad(esServicio ? "u" : unidad);
        result.validos.add(item);
    }

    private ParseResult parsearNumero(String raw, String campo) {
        if (esErrorCelda(raw)) {
            return new ParseResult(0, campo + " con error de celda/fórmula");
        }
        if (raw.equalsIgnoreCase("NaN")) {
            return new ParseResult(0, campo + " inválido (NaN)");
        }
        try {
            double val = Double.parseDouble(raw.replace(",", "."));
            if (Double.isNaN(val)) return new ParseResult(0, campo + " inválido (NaN)");
            if (Double.isInfinite(val)) return new ParseResult(0, campo + " inválido (Infinito)");
            return new ParseResult(val, null);
        } catch (NumberFormatException e) {
            return new ParseResult(0, campo + " no es un número ('" + raw + "')");
        }
    }

    private String leerString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case NUMERIC -> {
                double num = cell.getNumericCellValue();
                if (Double.isNaN(num) || Double.isInfinite(num)) yield "NaN";
                yield num % 1 == 0 ? String.valueOf((long) num) : String.valueOf(num);
            }
            case BOOLEAN -> cell.getBooleanCellValue() ? "SI" : "NO";
            case FORMULA -> {
                try {
                    yield switch (cell.getCachedFormulaResultType()) {
                        case NUMERIC -> {
                            double num = cell.getNumericCellValue();
                            if (Double.isNaN(num) || Double.isInfinite(num)) yield "NaN";
                            yield num % 1 == 0 ? String.valueOf((long) num) : String.valueOf(num);
                        }
                        case BOOLEAN -> cell.getBooleanCellValue() ? "SI" : "NO";
                        case STRING -> cell.getStringCellValue().trim();
                        case ERROR -> "##Error##";
                        default -> "";
                    };
                } catch (Exception e) {
                    yield "##Error##";
                }
            }
            case ERROR -> "##Error##";
            case BLANK -> "";
            default -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) { yield ""; }
            }
        };
    }

    private boolean esErrorCelda(String val) {
        return val.startsWith("##");
    }

    private boolean esFilaVacia(Row row) {
        for (int i = 0; i <= 7; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private boolean parsearEsServicio(String val) {
        return val.equalsIgnoreCase("SI") || val.equalsIgnoreCase("SÍ") ||
               val.equalsIgnoreCase("S") || val.equalsIgnoreCase("TRUE") || val.equals("1");
    }
}
