package com.agroruta.notification.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/** Enlace para confirmar el correo al registrarse. Se guarda solo el hash del token; vence a las 24 horas. */
@Getter
public class VerificacionCorreo {

    private final Long id;
    private final Long usuarioId;
    private final String tokenHash;
    private final LocalDateTime creadoEn;
    private final LocalDateTime expiraEn;
    private LocalDateTime usadoEn;

    public VerificacionCorreo(Long id, Long usuarioId, String tokenHash,
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
