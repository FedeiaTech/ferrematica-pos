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
    private int anchoTicketMm = 80;
    private boolean ticketMostrarDireccion = true;
    private boolean ticketMostrarCuit = true;
    private boolean usarEnteros = false;
    private double margenGananciaPct = 0.0;

    // Sincronización con Supabase
    private String supabaseUrl;
    private String supabaseAnonKey;
    private boolean supabaseSyncHabilitado = false;
    private int supabaseSyncIntervaloMin = 15;
    private String supabaseSyncEmail;
    private String supabaseSyncPassword;
    private String supabaseUltimaSyncExitosa;

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

    public int getAnchoTicketMm() { return anchoTicketMm; }
    public void setAnchoTicketMm(int v) { this.anchoTicketMm = v; }

    public boolean isTicketMostrarDireccion() { return ticketMostrarDireccion; }
    public void setTicketMostrarDireccion(boolean v) { this.ticketMostrarDireccion = v; }

    public boolean isTicketMostrarCuit() { return ticketMostrarCuit; }
    public void setTicketMostrarCuit(boolean v) { this.ticketMostrarCuit = v; }

    public boolean isUsarEnteros() { return usarEnteros; }
    public void setUsarEnteros(boolean v) { this.usarEnteros = v; }

    public double getMargenGananciaPct() { return margenGananciaPct; }
    public void setMargenGananciaPct(double v) { this.margenGananciaPct = v; }

    public String getSupabaseUrl() { return supabaseUrl; }
    public void setSupabaseUrl(String v) { this.supabaseUrl = v; }

    public String getSupabaseAnonKey() { return supabaseAnonKey; }
    public void setSupabaseAnonKey(String v) { this.supabaseAnonKey = v; }

    public boolean isSupabaseSyncHabilitado() { return supabaseSyncHabilitado; }
    public void setSupabaseSyncHabilitado(boolean v) { this.supabaseSyncHabilitado = v; }

    public int getSupabaseSyncIntervaloMin() { return supabaseSyncIntervaloMin; }
    public void setSupabaseSyncIntervaloMin(int v) { this.supabaseSyncIntervaloMin = v; }

    public String getSupabaseSyncEmail() { return supabaseSyncEmail; }
    public void setSupabaseSyncEmail(String v) { this.supabaseSyncEmail = v; }

    public String getSupabaseSyncPassword() { return supabaseSyncPassword; }
    public void setSupabaseSyncPassword(String v) { this.supabaseSyncPassword = v; }

    /** Instant.toString() de la última sincronización exitosa, o null si nunca sincronizó. */
    public String getSupabaseUltimaSyncExitosa() { return supabaseUltimaSyncExitosa; }
    public void setSupabaseUltimaSyncExitosa(String v) { this.supabaseUltimaSyncExitosa = v; }
}
