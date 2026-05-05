package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.dao.ConfiguracionDAO;
import com.fedeiatech.sistemagestionpyme.model.Configuracion;
import org.mindrot.jbcrypt.BCrypt;

public class LicenseService {

    private static final String HASH_CLAVE = "$2a$12$gn2XODzzLKiJ9EpPim9af.5oiQ9lQInTlpit0ZCNcXWi71R356oJG";

    private static Boolean premiumCache = null;

    public static boolean esPremium() {
        if (premiumCache != null) return premiumCache;
        try {
            Configuracion c = new ConfiguracionDAO().obtenerConfiguracion();
            premiumCache = (c != null && c.isPremiumDesbloqueado());
        } catch (Exception e) {
            premiumCache = false;
        }
        return premiumCache;
    }

    public static void invalidarCache() {
        premiumCache = null;
    }

    public static boolean verificarYDesbloquear(String clave) {
        if (clave == null || clave.isBlank()) return false;
        if (!BCrypt.checkpw(clave, HASH_CLAVE)) return false;
        try {
            new ConfiguracionDAO().desbloquearPremium();
            invalidarCache();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean permiteReportes() { return esPremium(); }
    public static boolean permiteEstadisticas() { return esPremium(); }
    public static boolean permiteExports() { return esPremium(); }
    public static boolean permiteGestionCombos() { return esPremium() && SessionService.getInstance().esAdmin(); }
    public static boolean permiteGestionUsuarios() { return esPremium() && SessionService.getInstance().esAdmin(); }
    public static boolean permiteModuloFiscal() { return esPremium() && SessionService.getInstance().esAdmin(); }
}
