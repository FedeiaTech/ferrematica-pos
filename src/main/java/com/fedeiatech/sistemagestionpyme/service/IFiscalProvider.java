package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.model.Venta;

public interface IFiscalProvider {

    ResultadoFiscal autorizar(Venta venta);

    boolean isServicioDisponible();
}
