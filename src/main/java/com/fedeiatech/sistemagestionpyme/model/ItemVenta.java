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
    private String unidad;
    private String categoria;
    private boolean esCombo = false;
    private int idCombo = 0;

    public ItemVenta() {
        this.unidad = "u";
        this.categoria = "General";
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
        this.unidad = "u";
        this.categoria = "General";
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

    public String getUnidad() { return unidad != null ? unidad : "u"; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public String getCategoria() { return categoria != null ? categoria : "General"; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public boolean isEsCombo() { return esCombo; }
    public void setEsCombo(boolean esCombo) { this.esCombo = esCombo; }

    public int getIdCombo() { return idCombo; }
    public void setIdCombo(int idCombo) { this.idCombo = idCombo; }

    public static ItemVenta desdeCombo(Combo combo) {
        ItemVenta iv = new ItemVenta();
        iv.setId(combo.getId());
        iv.setIdCombo(combo.getId());
        iv.setCodigo(combo.getCodigo());
        iv.setNombre(combo.getNombre());
        iv.setDescripcion(combo.getDescripcion());
        iv.setPrecioVenta(combo.getPrecioVenta());
        iv.setStock(combo.getStockCalculado());
        iv.setEsServicio(false);
        iv.setUnidad("u");
        iv.setEsCombo(true);
        return iv;
    }

    public boolean esPorPeso() {
        String u = getUnidad();
        return u.equals("kg") || u.equals("g") || u.equals("lt");
    }

    public double calcularGanancia() {
        return precioVenta - precioCosto;
    }

    @Override
    public String toString() {
        return codigo + " | " + nombre + " ($ " + precioVenta + ")";
    }
}
