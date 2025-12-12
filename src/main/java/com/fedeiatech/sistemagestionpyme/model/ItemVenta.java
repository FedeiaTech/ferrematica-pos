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

    // 1. Constructor Vacio (Necesario para herramientas y buenas prácticas)
    public ItemVenta() {
    }

    // 2. Constructor Completo (Para crear objetos rápido en el Main)
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

    // 3. Getters y Setters (Lo que Lombok falló en crear)
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

    // Lógica de negocio
    public double calcularGanancia() {
        return precioVenta - precioCosto;
    }
}