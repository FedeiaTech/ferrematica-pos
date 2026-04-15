package com.fedeiatech.sistemagestionpyme.service;

public class ResultadoFiscal {

    private boolean aprobado;
    private String cae;
    private String vencimientoCae;
    private String observaciones;
    private String numeroComprobante;

    public ResultadoFiscal() {
    }

    public ResultadoFiscal(String cae, String vencimientoCae, String numeroComprobante) {
        this.aprobado = true;
        this.cae = cae;
        this.vencimientoCae = vencimientoCae;
        this.numeroComprobante = numeroComprobante;
        this.observaciones = "Aprobado";
    }

    public static ResultadoFiscal error(String mensaje) {
        ResultadoFiscal r = new ResultadoFiscal();
        r.setAprobado(false);
        r.setObservaciones(mensaje);
        return r;
    }

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
