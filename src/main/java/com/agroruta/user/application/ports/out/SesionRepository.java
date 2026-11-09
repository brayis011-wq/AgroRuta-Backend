package com.agroruta.user.application.ports.out;

import com.agroruta.user.domain.Sesion;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SesionRepository {
    Sesion guardar(Sesion sesion);
    Optional<Sesion> buscarPorTokenHash(String tokenHash);
    /** Cierra todas las sesiones abiertas del usuario (cambio de contraseña o token robado). */
    void revocarTodas(Long usuarioId, LocalDateTime ahora);
    /** Limpieza: borra las sesiones del usuario que ya vencieron. */
    void borrarVencidas(Long usuarioId, LocalDateTime ahora);
}
