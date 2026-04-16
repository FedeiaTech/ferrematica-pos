package com.fedeiatech.sistemagestionpyme.model;

public class Usuario {

    public enum Rol { ADMIN, CAJERO }

    private int id;
    private String nombre;
    private String passwordHash;
    private Rol rol;

    public Usuario() {}

    public Usuario(String nombre, String passwordHash, Rol rol) {
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public boolean esAdmin() { return rol == Rol.ADMIN; }

    @Override
    public String toString() { return nombre + " (" + rol + ")"; }
}
