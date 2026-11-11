package com.agroruta.user.application.ports.out;

import com.agroruta.user.domain.Sesion;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SesionRepository {
    Sesion guardar(Sesion sesion);
    Optional<Sesion> buscarPorTokenHash(String tokenHash);
    /** Cierra todas las sesiones abiertas del usuario (cambio de contraseña o token robado). */
    void revocarTodas(Long usuarioId, LocalDateTime ahora);
    /** Sesiones del usuario que siguen abiertas (ni cerradas, ni reemplazadas, ni vencidas). */
    java.util.List<Sesion> buscarAbiertas(Long usuarioId, LocalDateTime ahora);

    /** Cierra un inicio de sesión completo (todas sus renovaciones). */
    void revocarFamilia(Long usuarioId, String familia, LocalDateTime ahora);

    /** Cierra todas las sesiones del usuario menos las de esa familia. Devuelve cuántas filas cerró. */
    int revocarOtras(Long usuarioId, String familia, LocalDateTime ahora);

    /** Limpieza: borra las sesiones del usuario que ya vencieron. */
    void borrarVencidas(Long usuarioId, LocalDateTime ahora);
}
