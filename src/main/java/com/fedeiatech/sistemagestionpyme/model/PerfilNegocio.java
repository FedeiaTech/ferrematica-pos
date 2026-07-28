package com.fedeiatech.sistemagestionpyme.model;

import java.util.List;

public enum PerfilNegocio {

    GENERICO("Genérico", List.of("General")),
    KIOSCO("Kiosco", List.of("Golosinas", "Bebidas", "Cigarrillos", "Almacén", "Snacks")),
    TIENDA("Tienda de ropa", List.of("Ropa", "Calzado", "Accesorios", "Indumentaria deportiva")),
    FERRETERIA("Ferretería", List.of("Tornillería", "Electricidad", "Pintura", "Herramientas", "Plomería"));

    private final String etiqueta;
    private final List<String> categoriasSugeridas;

    PerfilNegocio(String etiqueta, List<String> categoriasSugeridas) {
        this.etiqueta = etiqueta;
        this.categoriasSugeridas = categoriasSugeridas;
    }

    public String getEtiqueta() { return etiqueta; }

    public List<String> categoriasSugeridas() { return categoriasSugeridas; }

    @Override
    public String toString() { return etiqueta; }

    public static PerfilNegocio desdeNombre(String nombre) {
        if (nombre == null) return GENERICO;
        try {
            return PerfilNegocio.valueOf(nombre);
        } catch (IllegalArgumentException e) {
            return GENERICO;
        }
    }
}
