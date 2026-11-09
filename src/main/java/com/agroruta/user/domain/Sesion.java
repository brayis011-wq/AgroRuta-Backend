package com.agroruta.user.domain;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Una sesión renovable (refresh token). Solo se guarda el hash del token.
 * Al renovarse queda "rotada" y se crea otra; si alguien presenta un token rotado
 * hace rato, es señal de que se lo robaron y se cierran todas las sesiones del usuario.
 */
public class Sesion {

    private final Long id;
    private final Long usuarioId;
    private final String tokenHash;
    private final LocalDateTime creadoEn;
    private final LocalDateTime expiraEn;
    private LocalDateTime rotadoEn;
    private LocalDateTime revocadoEn;

    public Sesion(Long id, Long usuarioId, String tokenHash, LocalDateTime creadoEn, LocalDateTime expiraEn,
                  LocalDateTime rotadoEn, LocalDateTime revocadoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
        this.rotadoEn = rotadoEn;
        this.revocadoEn = revocadoEn;
    }

    public static Sesion nueva(Long usuarioId, String tokenHash, LocalDateTime ahora, Duration duracion) {
        return new Sesion(null, usuarioId, tokenHash, ahora, ahora.plus(duracion), null, null);
    }

    public boolean vencida(LocalDateTime ahora) {
        return !ahora.isBefore(expiraEn);
    }

    public boolean revocada() {
        return revocadoEn != null;
    }

    public boolean rotada() {
        return rotadoEn != null;
    }

    /** Dos pestañas pueden renovar casi a la vez con el mismo token: eso no es un robo. */
    public boolean rotadaHaceMenosDe(Duration margen, LocalDateTime ahora) {
        return rotadoEn != null && rotadoEn.plus(margen).isAfter(ahora);
    }

    public void rotar(LocalDateTime ahora) {
        if (rotadoEn == null) rotadoEn = ahora;
    }

    public void revocar(LocalDateTime ahora) {
        if (revocadoEn == null) revocadoEn = ahora;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public LocalDateTime getRotadoEn() { return rotadoEn; }
    public LocalDateTime getRevocadoEn() { return revocadoEn; }
}
