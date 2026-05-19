package com.agroruta.configuration.domain;

import com.agroruta.user.domain.Rol;

public class Perfil {

    private Long   id;
    private String nombre;
    private String email;
    private String telefono;
    private String fotoPerfil;
    private Rol    rol;

    public Perfil() {}

    public Perfil(Long id, String nombre, String email,
                  String telefono, String fotoPerfil, Rol rol) {
        this.id         = id;
        this.nombre     = nombre;
        this.email      = email;
        this.telefono   = telefono;
        this.fotoPerfil = fotoPerfil;
        this.rol        = rol;
    }

    public void actualizarDatos(String nombre, String telefono, String fotoPerfil) {
        this.nombre     = nombre;
        this.telefono   = telefono;
        this.fotoPerfil = fotoPerfil;
    }

    public Long   getId()                         { return id; }
    public void   setId(Long id)                  { this.id = id; }
    public String getNombre()                     { return nombre; }
    public void   setNombre(String nombre)        { this.nombre = nombre; }
    public String getEmail()                      { return email; }
    public void   setEmail(String email)          { this.email = email; }
    public String getTelefono()                   { return telefono; }
    public void   setTelefono(String t)           { this.telefono = t; }
    public String getFotoPerfil()                 { return fotoPerfil; }
    public void   setFotoPerfil(String f)         { this.fotoPerfil = f; }
    public Rol    getRol()                        { return rol; }
    public void   setRol(Rol rol)                 { this.rol = rol; }
}