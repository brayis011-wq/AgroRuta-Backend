package com.agroruta.user.application.ports.in;

/**
 * Resultado de registrarse, iniciar sesión o renovarla.
 *
 * @param token                 JWT de acceso; null si falta confirmar el correo
 * @param verificacionPendiente la cuenta se creó pero falta confirmar el correo
 */
public record SesionIniciada(Long id, String token, String email, String nombre, String rol,
                             boolean verificacionPendiente) {

    public SesionIniciada(Long id, String token, String email, String nombre, String rol) {
        this(id, token, email, nombre, rol, false);
    }
}
