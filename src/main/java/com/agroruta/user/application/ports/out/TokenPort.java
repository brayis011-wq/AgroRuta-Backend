package com.agroruta.user.application.ports.out;

import com.agroruta.user.domain.Usuario;

/** Emite el token de acceso de una persona. */
public interface TokenPort {
    String generarToken(Usuario usuario);
}
