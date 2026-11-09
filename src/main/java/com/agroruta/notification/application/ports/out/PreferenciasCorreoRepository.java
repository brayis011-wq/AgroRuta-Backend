package com.agroruta.notification.application.ports.out;

import com.agroruta.notification.domain.PreferenciasCorreo;

import java.util.Optional;

public interface PreferenciasCorreoRepository {
    Optional<PreferenciasCorreo> buscar(Long usuarioId);
    PreferenciasCorreo guardar(PreferenciasCorreo preferencias);
}
