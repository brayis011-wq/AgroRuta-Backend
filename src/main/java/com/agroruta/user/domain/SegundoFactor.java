package com.agroruta.user.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * El segundo factor de una cuenta. Mientras {@code activo} es false, la persona está en medio de
 * la configuración (ya escaneó el QR pero falta que escriba un código para confirmar).
 */
public class SegundoFactor {

    private final Long usuarioId;
    /** Secreto de la app de autenticación, cifrado */
    private final String secretoCifrado;
    private boolean activo;
    /** Hashes de los códigos de respaldo que quedan (cada uno sirve una sola vez) */
    private final List<String> respaldo;
    private Long ultimoPaso;
    private final LocalDateTime creadoEn;
    private LocalDateTime activadoEn;

    public SegundoFactor(Long usuarioId, String secretoCifrado, boolean activo, List<String> respaldo,
                         Long ultimoPaso, LocalDateTime creadoEn, LocalDateTime activadoEn) {
        this.usuarioId = usuarioId;
        this.secretoCifrado = secretoCifrado;
        this.activo = activo;
        this.respaldo = new ArrayList<>(respaldo == null ? List.of() : respaldo);
        this.ultimoPaso = ultimoPaso;
        this.creadoEn = creadoEn;
        this.activadoEn = activadoEn;
    }

    public static SegundoFactor pendiente(Long usuarioId, String secretoCifrado, LocalDateTime ahora) {
        return new SegundoFactor(usuarioId, secretoCifrado, false, List.of(), null, ahora, null);
    }

    public void activar(long paso, List<String> hashesRespaldo, LocalDateTime ahora) {
        this.activo = true;
        this.ultimoPaso = paso;
        this.activadoEn = ahora;
        this.respaldo.clear();
        this.respaldo.addAll(hashesRespaldo);
    }

    public void usarPaso(long paso) {
        this.ultimoPaso = paso;
    }

    /** Gasta un código de respaldo. false si no era uno de los que quedaban. */
    public boolean usarRespaldo(String hash) {
        return respaldo.remove(hash);
    }

    public Long getUsuarioId() { return usuarioId; }
    public String getSecretoCifrado() { return secretoCifrado; }
    public boolean isActivo() { return activo; }
    public List<String> getRespaldo() { return List.copyOf(respaldo); }
    public Long getUltimoPaso() { return ultimoPaso; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActivadoEn() { return activadoEn; }
}
