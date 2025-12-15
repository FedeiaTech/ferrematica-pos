package com.fedeiatech.sistemagestionpyme.service;

/**
 * Servicio de control de licencias.
 * Gestiona qué características están activas según el plan.
 */
public class LicenseService {

    // CAMBIAR A 'false' PARA PROBAR EL BLOQUEO, 'true' PARA DESARROLLAR
    private static boolean ES_PREMIUM = true; 

    public static boolean esPremium() {
        return ES_PREMIUM;
    }
    
    public static void setPremium(boolean estado) {
        ES_PREMIUM = estado;
    }
    
    // [PREMIUM] Reportes Avanzados
    public static boolean permiteReportes() {
        return ES_PREMIUM;
    }
    
    // [PREMIUM] Conexión ARCA
    public static boolean permiteModuloFiscal() {
        return ES_PREMIUM;
    }
}