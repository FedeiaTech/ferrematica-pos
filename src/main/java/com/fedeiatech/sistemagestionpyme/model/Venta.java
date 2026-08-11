package com.fedeiatech.sistemagestionpyme.model;

import java.util.ArrayList;
import java.util.List;

public class Venta {

    public static final String ESTADO_COMPLETADA = "completada";
    public static final String ESTADO_ANULADA = "anulada";

    private int id;
    private String fecha;
    private double total;
    private List<DetalleVenta> detalles;
    private String estado;
    private String motivoAnulacion;

    public Venta() {
        this.detalles = new ArrayList<>();
        this.total = 0.0;
        this.estado = ESTADO_COMPLETADA;
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

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public boolean estaAnulada() { return ESTADO_ANULADA.equals(estado); }
}
