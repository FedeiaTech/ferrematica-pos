package com.fedeiatech.sistemagestionpyme.model;

public class Configuracion {
    private String nombreEmpresa;
    private String cuit;
    private String direccion;
    private String condicionIva;
    private int puntoVenta;
    private String certificadoRuta;

    // Constructor vacío
    public Configuracion() {
    }

    // Constructor completo
    public Configuracion(String nombreEmpresa, String cuit, String direccion, String condicionIva, int puntoVenta) {
        this.nombreEmpresa = nombreEmpresa;
        this.cuit = cuit;
        this.direccion = direccion;
        this.condicionIva = condicionIva;
        this.puntoVenta = puntoVenta;
    }

    // --- GETTERS Y SETTERS ---
    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCondicionIva() { return condicionIva; }
    public void setCondicionIva(String condicionIva) { this.condicionIva = condicionIva; }

    public int getPuntoVenta() { return puntoVenta; }
    public void setPuntoVenta(int puntoVenta) { this.puntoVenta = puntoVenta; }

    public String getCertificadoRuta() { return certificadoRuta; }
    public void setCertificadoRuta(String certificadoRuta) { this.certificadoRuta = certificadoRuta; }
}