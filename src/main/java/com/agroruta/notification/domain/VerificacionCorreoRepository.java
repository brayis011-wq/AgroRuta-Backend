package com.agroruta.notification.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificacionCorreoRepository {
    VerificacionCorreo guardar(VerificacionCorreo v);
    Optional<VerificacionCorreo> buscarPorTokenHash(String tokenHash);
    long contarDesde(Long usuarioId, LocalDateTime desde);
    void anularPendientes(Long usuarioId, LocalDateTime ahora);
}
