package com.fedeiatech.sistemagestionpyme.model;

public class DetalleVenta {
    private int id;
    private int idVenta;
    private ItemVenta item; // Guardamos el objeto Item completo para saber su nombre
    private double cantidad;
    private double precioUnitario;
    
    // Constructor vacío
    public DetalleVenta() {}

    // Constructor útil
    public DetalleVenta(ItemVenta item, double cantidad) {
        this.item = item;
        this.cantidad = cantidad;
        this.precioUnitario = item.getPrecioVenta(); // El precio se congela al momento de la venta
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public ItemVenta getItem() { return item; }
    public void setItem(ItemVenta item) { this.item = item; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    // Calculado: Cantidad * Precio
    public double getSubtotal() {
        return cantidad * precioUnitario;
    }
    
    // Esto permite que PropertyValueFactory use "codigoItem"
    public String getCodigoItem() {
        return (item != null) ? item.getCodigo() : "";
    }

    // Esto permite que PropertyValueFactory use "nombreItem"
    public String getNombreItem() {
        return (item != null) ? item.getNombre() : "";
    }
}