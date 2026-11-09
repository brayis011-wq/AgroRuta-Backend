package com.agroruta.user.application.ports.in;

import com.agroruta.user.domain.Usuario;

/** Sesiones renovables: el token largo vive en una cookie httpOnly y sirve para pedir tokens de acceso. */
public interface SesionUseCase {

    /** Abre una sesión y devuelve el token que va en la cookie. */
    String abrir(Long usuarioId);

    /** Cambia el token de la cookie por uno nuevo. Lanza SesionInvalidaException si no sirve. */
    Renovacion renovar(String token);

    /** Cierra la sesión de ese token (si existe). */
    void cerrar(String token);

    record Renovacion(Usuario usuario, String token) {}
}
