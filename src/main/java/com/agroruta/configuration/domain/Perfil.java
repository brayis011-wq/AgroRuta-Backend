package com.agroruta.configuration.domain;

import com.agroruta.user.domain.Rol;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Perfil {

    private Long   id;
    private String nombre;
    private String email;
    private String telefono;
    private String fotoPerfil;
    private Rol    rol;

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
}