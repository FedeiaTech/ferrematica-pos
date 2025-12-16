package com.fedeiatech.sistemagestionpyme.model;

public class DetalleVenta {
    private ItemVenta item;
    private double cantidad; // Ahora usamos double para permitir 1.5 kg, etc.
    private double precioUnitario; // IMPORTANTE: Guardamos el precio histórico aquí

    // Constructor vacío
    public DetalleVenta() {
    }

    // Constructor para Venta Nueva (toma el precio actual del item)
    public DetalleVenta(ItemVenta item, double cantidad) {
        this.item = item;
        this.cantidad = cantidad;
        this.precioUnitario = item.getPrecioVenta(); // Congelamos el precio al momento de crear
    }

    // --- GETTERS Y SETTERS ---

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

    // Usado para cargar ventas viejas con precios viejos
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    // --- SUBTOTAL CALCULADO (Sin Setter) ---
    // El subtotal siempre es el resultado de la matemática, no se asigna manualmente.
    public double getSubtotal() {
        return this.cantidad * this.precioUnitario;
    }
    
    // Getters auxiliares para la Tabla (TableView usa PropertyValueFactory busca "nombreItem")
    public String getCodigoItem() {
        return item != null ? item.getCodigo() : "";
    }
    
    public String getNombreItem() {
        return item != null ? item.getNombre() : "";
    }
}