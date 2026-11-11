package com.agroruta.user.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una sesión renovable (refresh token). Solo se guarda el hash del token.
 * Al renovarse queda "rotada" y se crea otra de la misma familia; si alguien presenta un token
 * rotado hace rato, es señal de que se lo robaron y se cierran todas las sesiones del usuario.
 *
 * La familia es el inicio de sesión: todas las renovaciones de un mismo login comparten familia,
 * dispositivo y fecha de inicio. Así se puede mostrar "sesión abierta en Chrome · Windows desde el 3 de
 * noviembre" y cerrarla a distancia.
 */
public class Sesion {

    private final Long id;
    private final Long usuarioId;
    private final String tokenHash;
    private final LocalDateTime creadoEn;
    private final LocalDateTime expiraEn;
    private LocalDateTime rotadoEn;
    private LocalDateTime revocadoEn;
    private final String familia;
    private final String dispositivo;
    private final LocalDateTime iniciadaEn;

    public Sesion(Long id, Long usuarioId, String tokenHash, LocalDateTime creadoEn, LocalDateTime expiraEn,
                  LocalDateTime rotadoEn, LocalDateTime revocadoEn) {
        this(id, usuarioId, tokenHash, creadoEn, expiraEn, rotadoEn, revocadoEn, null, null, null);
    }

    public Sesion(Long id, Long usuarioId, String tokenHash, LocalDateTime creadoEn, LocalDateTime expiraEn,
                  LocalDateTime rotadoEn, LocalDateTime revocadoEn,
                  String familia, String dispositivo, LocalDateTime iniciadaEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
        this.rotadoEn = rotadoEn;
        this.revocadoEn = revocadoEn;
        this.familia = familia;
        this.dispositivo = dispositivo;
        this.iniciadaEn = iniciadaEn;
    }

    public static Sesion nueva(Long usuarioId, String tokenHash, LocalDateTime ahora, Duration duracion) {
        return nueva(usuarioId, tokenHash, ahora, duracion, null);
    }

    /** Un inicio de sesión: familia nueva */
    public static Sesion nueva(Long usuarioId, String tokenHash, LocalDateTime ahora, Duration duracion, String dispositivo) {
        return new Sesion(null, usuarioId, tokenHash, ahora, ahora.plus(duracion), null, null,
                UUID.randomUUID().toString(), dispositivo, ahora);
    }

    /** La sesión que reemplaza a esta al renovar: misma familia, dispositivo e inicio */
    public Sesion siguiente(String nuevoTokenHash, LocalDateTime ahora, Duration duracion) {
        return new Sesion(null, usuarioId, nuevoTokenHash, ahora, ahora.plus(duracion), null, null,
                familia != null ? familia : UUID.randomUUID().toString(), dispositivo,
                iniciadaEn != null ? iniciadaEn : creadoEn);
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

    /** Sigue sirviendo para renovar: ni cerrada, ni reemplazada, ni vencida */
    public boolean abierta(LocalDateTime ahora) {
        return !revocada() && !rotada() && !vencida(ahora);
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
    public String getFamilia() { return familia; }
    public String getDispositivo() { return dispositivo; }
    /** Cuándo se inició la sesión (el login); en filas antiguas, cuando se creó la fila */
    public LocalDateTime getIniciadaEn() { return iniciadaEn != null ? iniciadaEn : creadoEn; }
}
