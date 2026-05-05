package com.fedeiatech.sistemagestionpyme.model;

import java.util.ArrayList;
import java.util.List;

public class Combo {
    private int id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private double precioVenta;
    private List<ComponenteCombo> componentes = new ArrayList<>();
    private int stockCalculado = 0;

    public Combo() {}

    public Combo(String codigo, String nombre, String descripcion, double precioVenta) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }

    public List<ComponenteCombo> getComponentes() { return componentes; }
    public void setComponentes(List<ComponenteCombo> componentes) { this.componentes = componentes; }
    public void agregarComponente(ComponenteCombo c) { this.componentes.add(c); }

    public int getStockCalculado() { return stockCalculado; }
    public void setStockCalculado(int stockCalculado) { this.stockCalculado = stockCalculado; }

    @Override
    public String toString() {
        return codigo + " | " + nombre + " ($ " + precioVenta + ")";
    }
}
