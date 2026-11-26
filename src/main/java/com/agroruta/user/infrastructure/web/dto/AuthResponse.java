package com.agroruta.user.infrastructure.web.dto;

import com.agroruta.user.application.ports.in.SesionIniciada;

/**
 * Respuesta de registro, inicio de sesión y renovación.
 *
 * @param verificacionPendiente true = la cuenta se creó pero falta confirmar el correo (no hay token todavía)
 */
public record AuthResponse(Long id, String token, String email, String nombre, String rol,
                           boolean verificacionPendiente) {

    public AuthResponse(Long id, String token, String email, String nombre, String rol) {
        this(id, token, email, nombre, rol, false);
    }

    public static AuthResponse de(SesionIniciada s) {
        return new AuthResponse(s.id(), s.token(), s.email(), s.nombre(), s.rol(), s.verificacionPendiente());
    }
}
