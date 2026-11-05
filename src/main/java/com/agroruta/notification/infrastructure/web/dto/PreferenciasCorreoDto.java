package com.agroruta.notification.infrastructure.web.dto;

import com.agroruta.notification.domain.PreferenciasCorreo;

public record PreferenciasCorreoDto(boolean alertasCultivo, boolean avisosTrabajadores) {
    public static PreferenciasCorreoDto de(PreferenciasCorreo p) {
        return new PreferenciasCorreoDto(p.alertasCultivo(), p.avisosTrabajadores());
    }
}
