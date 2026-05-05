package com.fedeiatech.sistemagestionpyme.model;

public class Configuracion {
    private String nombreEmpresa;
    private String cuit;
    private String direccion;
    private String condicionIva;
    private int puntoVenta;
    private String certificadoRuta;
    private String rutaLogo;
    private String mensajeTicket;
    private boolean permitirStockNegativo;
    private double recargoTarjeta;
    private String rutaBackup;
    private String rutaGuardadoTickets;
    private String colorTema = "#f4f6f8";
    private boolean premiumDesbloqueado = false;

    public Configuracion() {
    }

    public Configuracion(String nombreEmpresa, String cuit, String direccion, String condicionIva, int puntoVenta) {
        this.nombreEmpresa = nombreEmpresa;
        this.cuit = cuit;
        this.direccion = direccion;
        this.condicionIva = condicionIva;
        this.puntoVenta = puntoVenta;
    }

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

    public String getRutaLogo() { return rutaLogo; }
    public void setRutaLogo(String rutaLogo) { this.rutaLogo = rutaLogo; }

    public String getMensajeTicket() { return mensajeTicket; }
    public void setMensajeTicket(String mensajeTicket) { this.mensajeTicket = mensajeTicket; }

    public boolean isPermitirStockNegativo() { return permitirStockNegativo; }
    public void setPermitirStockNegativo(boolean permitirStockNegativo) { this.permitirStockNegativo = permitirStockNegativo; }

    public double getRecargoTarjeta() { return recargoTarjeta; }
    public void setRecargoTarjeta(double recargoTarjeta) { this.recargoTarjeta = recargoTarjeta; }

    public String getRutaBackup() { return rutaBackup; }
    public void setRutaBackup(String rutaBackup) { this.rutaBackup = rutaBackup; }

    public String getRutaGuardadoTickets() {
        return rutaGuardadoTickets;
    }

    public void setRutaGuardadoTickets(String rutaGuardadoTickets) {
        this.rutaGuardadoTickets = rutaGuardadoTickets;
    }

    public String getColorTema() { return colorTema; }
    public void setColorTema(String colorTema) { this.colorTema = colorTema; }

    public boolean isPremiumDesbloqueado() { return premiumDesbloqueado; }
    public void setPremiumDesbloqueado(boolean premiumDesbloqueado) { this.premiumDesbloqueado = premiumDesbloqueado; }
}
