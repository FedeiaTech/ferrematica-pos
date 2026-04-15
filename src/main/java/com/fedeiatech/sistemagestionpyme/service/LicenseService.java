package com.fedeiatech.sistemagestionpyme.service;

public class LicenseService {

    private static boolean ES_PREMIUM = true;

    public static boolean esPremium() {
        return ES_PREMIUM;
    }

    public static void setPremium(boolean estado) {
        ES_PREMIUM = estado;
    }

    public static boolean permiteReportes() {
        return ES_PREMIUM;
    }

    public static boolean permiteModuloFiscal() {
        return ES_PREMIUM;
    }
}
