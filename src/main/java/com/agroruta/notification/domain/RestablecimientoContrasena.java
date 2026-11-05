package com.agroruta.notification.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Un enlace para crear una contraseña nueva. Se guarda solo el hash del token (si alguien lee la
 * base, no puede usar los enlaces), vence a los 30 minutos y sirve una sola vez.
 */
@Getter
public class RestablecimientoContrasena {

    private final Long id;
    private final Long usuarioId;
    private final String tokenHash;
    private final LocalDateTime creadoEn;
    private final LocalDateTime expiraEn;
    private LocalDateTime usadoEn;

    public RestablecimientoContrasena(Long id, Long usuarioId, String tokenHash,
                                      LocalDateTime creadoEn, LocalDateTime expiraEn, LocalDateTime usadoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
        this.usadoEn = usadoEn;
    }

    public boolean sirve(LocalDateTime ahora) {
        return usadoEn == null && ahora.isBefore(expiraEn);
    }

    public void usar(LocalDateTime ahora) {
        this.usadoEn = ahora;
    }
}
