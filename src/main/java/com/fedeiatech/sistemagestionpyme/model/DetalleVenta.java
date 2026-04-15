package com.fedeiatech.sistemagestionpyme.model;

public class DetalleVenta {
    private ItemVenta item;
    private double cantidad;
    private double precioUnitario;

    public DetalleVenta() {
    }

    public DetalleVenta(ItemVenta item, double cantidad) {
        this.item = item;
        this.cantidad = cantidad;
        this.precioUnitario = item.getPrecioVenta();
    }

    public ItemVenta getItem() {
        return item;
    }

    public void setItem(ItemVenta item) {
        this.item = item;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getSubtotal() {
        return this.cantidad * this.precioUnitario;
    }

    public String getCodigoItem() {
        return item != null ? item.getCodigo() : "";
    }

    public String getNombreItem() {
        return item != null ? item.getNombre() : "";
    }
}
