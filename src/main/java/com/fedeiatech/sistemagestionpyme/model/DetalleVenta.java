package com.fedeiatech.sistemagestionpyme.model;

public class DetalleVenta {
    private ItemVenta item;
    private Combo combo;
    private double cantidad;
    private double precioUnitario;

    public DetalleVenta() {}

    public DetalleVenta(ItemVenta item, double cantidad) {
        this.item = item;
        this.cantidad = cantidad;
        this.precioUnitario = item.getPrecioVenta();
    }

    public DetalleVenta(Combo combo, double cantidad) {
        this.combo = combo;
        this.cantidad = cantidad;
        this.precioUnitario = combo.getPrecioVenta();
    }

    public boolean esCombo() { return combo != null; }

    public ItemVenta getItem() { return item; }
    public void setItem(ItemVenta item) { this.item = item; }

    public Combo getCombo() { return combo; }
    public void setCombo(Combo combo) { this.combo = combo; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public double getSubtotal() { return cantidad * precioUnitario; }

    public String getCodigoItem() {
        if (esCombo()) return combo.getCodigo();
        return item != null ? item.getCodigo() : "";
    }

    public String getNombreItem() {
        if (esCombo()) return combo.getNombre();
        return item != null ? item.getNombre() : "";
    }
}
