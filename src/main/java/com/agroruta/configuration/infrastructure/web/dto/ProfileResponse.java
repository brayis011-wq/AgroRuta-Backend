package com.agroruta.configuration.infrastructure.web.dto;

import com.agroruta.configuration.domain.Perfil;
import com.agroruta.user.domain.Rol;

public class ProfileResponse {

    private Long   id;
    private String nombre;
    private String email;
    private String telefono;
    private String fotoPerfil;
    private Rol    rol;

    public static ProfileResponse from(Perfil p) {
        ProfileResponse r = new ProfileResponse();
        r.id         = p.getId();
        r.nombre     = p.getNombre();
        r.email      = p.getEmail();
        r.telefono   = p.getTelefono();
        r.fotoPerfil = p.getFotoPerfil();
        r.rol        = p.getRol();
        return r;
    }

    public Long   getId()         { return id; }
    public String getNombre()     { return nombre; }
    public String getEmail()      { return email; }
    public String getTelefono()   { return telefono; }
    public String getFotoPerfil() { return fotoPerfil; }
    public Rol    getRol()        { return rol; }
}