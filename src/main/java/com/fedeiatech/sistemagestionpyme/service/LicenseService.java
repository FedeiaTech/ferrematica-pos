package com.fedeiatech.sistemagestionpyme.service;

public class LicenseService {

    public static boolean esPremium() {
        return SessionService.getInstance().esAdmin();
    }

    public static boolean permiteReportes() {
        return SessionService.getInstance().esAdmin();
    }

    public static boolean permiteModuloFiscal() {
        return SessionService.getInstance().esAdmin();
    }
}
