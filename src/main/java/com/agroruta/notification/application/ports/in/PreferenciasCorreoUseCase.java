package com.agroruta.notification.application.ports.in;

import com.agroruta.notification.domain.PreferenciasCorreo;

public interface PreferenciasCorreoUseCase {
    PreferenciasCorreo obtener(Long usuarioId);
    PreferenciasCorreo guardar(Long usuarioId, boolean alertasCultivo, boolean avisosTrabajadores);
}
