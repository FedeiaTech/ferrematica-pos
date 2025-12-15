package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.model.Venta;
import java.time.LocalDate;
import java.util.Random;

/**
 * Proveedor Falso para pruebas y desarrollo.
 * Simula que ARCA responde siempre bien.
 */
public class MockFiscalProvider implements IFiscalProvider {

    @Override
    public ResultadoFiscal autorizar(Venta venta) {
        System.out.println("--- SIMULANDO CONEXIÓN CON ARCA ---");
        System.out.println("Enviando venta de monto: $" + venta.getTotal());
        
        // Simulamos una pequeña demora de internet (500ms)
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Generamos datos falsos
        String caeFalso = "74" + (1000000000000L + new Random().nextLong());
        String vencimiento = LocalDate.now().plusDays(10).toString();
        String numeroComprobante = String.valueOf(System.currentTimeMillis() % 10000);

        return new ResultadoFiscal(caeFalso, vencimiento, numeroComprobante);
    }

    @Override
    public boolean isServicioDisponible() {
        return true; // Siempre online
    }
}