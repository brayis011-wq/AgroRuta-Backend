package com.agroruta.user.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    private Long id;
    private String nombre;
    private String email;
    private String password;
    private Rol rol;
    private String telefono;
    private String fotoPerfil;
    private boolean activo;
    private LocalDateTime fechaCreacion;

    public Usuario(Long id, String nombre, String email,
                   String password, Rol rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public void desactivar() { this.activo = false; }
    public void activar()    { this.activo = true; }
}