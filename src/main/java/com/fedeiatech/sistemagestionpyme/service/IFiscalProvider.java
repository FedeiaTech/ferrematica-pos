package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.model.Venta;

/**
 * Puerto (Interface) para proveedores fiscales.
 * El sistema no sabe si esto conecta con ARCA o un simulador.
 */
public interface IFiscalProvider {
    
    /**
     * Intenta autorizar una venta ante el ente regulador.
     * * @param venta El objeto venta con sus detalles y cliente.
     * @return ResultadoFiscal con el CAE o el error.
     */
    ResultadoFiscal autorizar(Venta venta);

    /**
     * Verifica si el servicio externo (ej. servidores de ARCA) responde.
     * Útil para mostrar un semáforo verde/rojo en el Dashboard.
     * * @return true si hay conexión.
     */
    boolean isServicioDisponible();
}