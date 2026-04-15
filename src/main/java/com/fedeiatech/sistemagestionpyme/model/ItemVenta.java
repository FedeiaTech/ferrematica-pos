package com.fedeiatech.sistemagestionpyme.model;

public class ItemVenta {

    private int id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private double precioCosto;
    private double precioVenta;
    private double stock;
    private boolean esServicio;

    public ItemVenta() {
    }

    public ItemVenta(int id, String codigo, String nombre, String descripcion, double precioCosto, double precioVenta, double stock, boolean esServicio) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioCosto = precioCosto;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.esServicio = esServicio;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecioCosto() { return precioCosto; }
    public void setPrecioCosto(double precioCosto) { this.precioCosto = precioCosto; }

    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }

    public double getStock() { return stock; }
    public void setStock(double stock) { this.stock = stock; }

    public boolean isEsServicio() { return esServicio; }
    public void setEsServicio(boolean esServicio) { this.esServicio = esServicio; }

    public double calcularGanancia() {
        return precioVenta - precioCosto;
    }

    @Override
    public String toString() {
        return codigo + " | " + nombre + " ($ " + precioVenta + ")";
    }
}
