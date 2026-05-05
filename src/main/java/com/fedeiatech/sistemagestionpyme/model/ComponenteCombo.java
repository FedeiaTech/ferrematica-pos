package com.fedeiatech.sistemagestionpyme.model;

public class ComponenteCombo {
    private int idItem;
    private String nombreItem;
    private String codigoItem;
    private double cantidad;

    public ComponenteCombo() {}

    public ComponenteCombo(int idItem, String codigoItem, String nombreItem, double cantidad) {
        this.idItem = idItem;
        this.codigoItem = codigoItem;
        this.nombreItem = nombreItem;
        this.cantidad = cantidad;
    }

    public int getIdItem() { return idItem; }
    public void setIdItem(int idItem) { this.idItem = idItem; }

    public String getNombreItem() { return nombreItem; }
    public void setNombreItem(String nombreItem) { this.nombreItem = nombreItem; }

    public String getCodigoItem() { return codigoItem; }
    public void setCodigoItem(String codigoItem) { this.codigoItem = codigoItem; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    @Override
    public String toString() {
        return codigoItem + " | " + nombreItem + " x" + cantidad;
    }
}
