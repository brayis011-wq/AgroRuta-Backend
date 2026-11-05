package com.agroruta.notification.domain;

import java.util.Optional;

public interface PreferenciasCorreoRepository {
    Optional<PreferenciasCorreo> buscar(Long usuarioId);
    PreferenciasCorreo guardar(PreferenciasCorreo preferencias);
}
