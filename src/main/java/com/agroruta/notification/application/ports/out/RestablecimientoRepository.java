package com.agroruta.notification.application.ports.out;

import com.agroruta.notification.domain.RestablecimientoContrasena;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RestablecimientoRepository {
    RestablecimientoContrasena guardar(RestablecimientoContrasena r);
    Optional<RestablecimientoContrasena> buscarPorTokenHash(String tokenHash);
    /** Cuántos enlaces se pidieron para el usuario desde una fecha (para no mandar correos sin fin) */
    long contarDesde(Long usuarioId, LocalDateTime desde);
    /** Los enlaces que aún no se usaron dejan de servir (al pedir uno nuevo o al cambiar la contraseña) */
    void anularPendientes(Long usuarioId, LocalDateTime ahora);
}
