package com.agroruta.user.infrastructure.web.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private Long id;
    private String token;
    private String email;
    private String nombre;
    private String rol;
    /** true = la cuenta se creó pero falta confirmar el correo (no hay token todavía) */
    private boolean verificacionPendiente;

    public AuthResponse(Long id, String token, String email, String nombre, String rol) {
        this(id, token, email, nombre, rol, false);
    }

    public AuthResponse(Long id, String token, String email, String nombre, String rol,
                        boolean verificacionPendiente) {
        this.id = id;
        this.token = token;
        this.email = email;
        this.nombre = nombre;
        this.rol = rol;
        this.verificacionPendiente = verificacionPendiente;
    }
}