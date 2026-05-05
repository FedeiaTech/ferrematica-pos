package com.fedeiatech.sistemagestionpyme.model;

import java.util.ArrayList;
import java.util.List;

public class Venta {
    private int id;
    private String fecha;
    private double total;
    private List<DetalleVenta> detalles;

    public Venta() {
        this.detalles = new ArrayList<>();
        this.total = 0.0;
    }

    public void agregarDetalle(DetalleVenta detalle) {
        this.detalles.add(detalle);
        calcularTotal();
    }

    public void calcularTotal() {
        this.total = 0;
        for (DetalleVenta d : detalles) {
            this.total += d.getSubtotal();
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public double getTotal() { return total; }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<DetalleVenta> getDetalles() { return detalles; }
}
