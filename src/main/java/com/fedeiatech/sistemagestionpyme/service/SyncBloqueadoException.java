package com.fedeiatech.sistemagestionpyme.service;

import java.util.List;

/**
 * Lanzada cuando la sincronización con Supabase se intenta ejecutar mientras existen
 * productos con código en blanco o duplicado. Es la puerta de entrada autoritativa —
 * se evalúa antes de cualquier llamada HTTP en {@link SupabaseSyncService#sincronizar()}.
 */
public class SyncBloqueadoException extends RuntimeException {

    private final List<String> codigosBloqueados;

    public SyncBloqueadoException(List<String> codigosBloqueados) {
        super(construirMensaje(codigosBloqueados));
        this.codigosBloqueados = codigosBloqueados;
    }

    public List<String> getCodigosBloqueados() {
        return codigosBloqueados;
    }

    private static String construirMensaje(List<String> codigos) {
        StringBuilder sb = new StringBuilder(
            "Sincronización bloqueada: corregí estos productos en Inventario (código vacío o duplicado):\n");
        for (String c : codigos) {
            sb.append("• ").append(c).append("\n");
        }
        return sb.toString();
    }
}
