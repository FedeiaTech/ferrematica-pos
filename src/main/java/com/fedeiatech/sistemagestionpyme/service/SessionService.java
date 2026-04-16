package com.fedeiatech.sistemagestionpyme.service;

import com.fedeiatech.sistemagestionpyme.model.Usuario;

public class SessionService {

    private static SessionService instancia;
    private Usuario usuarioActivo;

    private SessionService() {}

    public static SessionService getInstance() {
        if (instancia == null) instancia = new SessionService();
        return instancia;
    }

    public void iniciarSesion(Usuario usuario) { this.usuarioActivo = usuario; }

    public void cerrarSesion() { this.usuarioActivo = null; }

    public Usuario getUsuarioActivo() { return usuarioActivo; }

    public boolean estaLogueado() { return usuarioActivo != null; }

    public boolean esAdmin() { return estaLogueado() && usuarioActivo.esAdmin(); }
}
