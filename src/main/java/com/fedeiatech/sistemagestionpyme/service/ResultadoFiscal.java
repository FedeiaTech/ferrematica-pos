package com.fedeiatech.sistemagestionpyme.service;

public class ResultadoFiscal {
    
    private boolean aprobado;
    private String cae;             // Código de Autorización Electrónico
    private String vencimientoCae;  // Fecha de vencimiento del CAE
    private String observaciones;   // Mensajes de error o advertencias (ej. "CUIT inválido")
    private String numeroComprobante; // El número final asignado (ej. 00000045)

    // Constructor vacío
    public ResultadoFiscal() {
    }

    // Constructor para aprobación exitosa
    public ResultadoFiscal(String cae, String vencimientoCae, String numeroComprobante) {
        this.aprobado = true;
        this.cae = cae;
        this.vencimientoCae = vencimientoCae;
        this.numeroComprobante = numeroComprobante;
        this.observaciones = "Aprobado";
    }

    // Constructor para rechazos o errores
    public static ResultadoFiscal error(String mensaje) {
        ResultadoFiscal r = new ResultadoFiscal();
        r.setAprobado(false);
        r.setObservaciones(mensaje);
        return r;
    }

    // Getters y Setters
    public boolean isAprobado() { return aprobado; }
    public void setAprobado(boolean aprobado) { this.aprobado = aprobado; }

    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }

    public String getVencimientoCae() { return vencimientoCae; }
    public void setVencimientoCae(String vencimientoCae) { this.vencimientoCae = vencimientoCae; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }
}