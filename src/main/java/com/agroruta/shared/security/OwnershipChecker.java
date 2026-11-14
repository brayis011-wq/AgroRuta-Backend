package com.agroruta.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Verifica que el usuario autenticado sea dueño del recurso solicitado.
 * Crop:   Finca (agricultorId) -> Lote (fincaId) -> Siembra (loteId) -> Cosecha, Fumigación, Actividad.
 * Worker: Trabajador y Cargo (agricultorId); Jornal, Nómina -> Trabajador; Pago -> Nómina.
 * Se usa desde @PreAuthorize("@ownership.xxx(#id, authentication)").
 * Si el recurso no existe o no es del usuario, devuelve false (403).
 * El Administrador siempre tiene acceso.
 * Cada verificación es una sola consulta ({@link ConsultaPropiedad}).
 */
@Component("ownership")
public class OwnershipChecker {

    private final ConsultaPropiedad consulta;

    public OwnershipChecker(ConsultaPropiedad consulta) {
        this.consulta = consulta;
    }

    /** El agricultorId recibido debe ser el del usuario autenticado. */
    public boolean esAgricultor(Long agricultorId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        return userId != null && userId.equals(agricultorId);
    }

    // ── crop ─────────────────────────────────────────────────────────────

    public boolean fincaPropia(Long fincaId, Authentication auth) {
        return propio(Recurso.FINCA, fincaId, auth);
    }

    public boolean lotePropio(Long loteId, Authentication auth) {
        return propio(Recurso.LOTE, loteId, auth);
    }

    public boolean siembraPropia(Long siembraId, Authentication auth) {
        return propio(Recurso.SIEMBRA, siembraId, auth);
    }

    public boolean cosechaPropia(Long cosechaId, Authentication auth) {
        return propio(Recurso.COSECHA, cosechaId, auth);
    }

    public boolean fumigacionPropia(Long fumigacionId, Authentication auth) {
        return propio(Recurso.FUMIGACION, fumigacionId, auth);
    }

    public boolean actividadPropia(Long actividadId, Authentication auth) {
        return propio(Recurso.ACTIVIDAD_CULTIVO, actividadId, auth);
    }

    // ── worker ───────────────────────────────────────────────────────────

    /** Labor de los jornales: el agricultor solo modifica las suyas; las comunes, solo el Administrador. */
    public boolean laborPropia(Long actividadId, Authentication auth) {
        return propio(Recurso.LABOR, actividadId, auth);
    }

    public boolean trabajadorPropio(Long trabajadorId, Authentication auth) {
        return propio(Recurso.TRABAJADOR, trabajadorId, auth);
    }

    public boolean cargoPropio(Long cargoId, Authentication auth) {
        return propio(Recurso.CARGO, cargoId, auth);
    }

    public boolean jornalPropio(Long jornalId, Authentication auth) {
        return propio(Recurso.JORNAL, jornalId, auth);
    }

    public boolean nominaPropia(Long nominaId, Authentication auth) {
        return propio(Recurso.NOMINA, nominaId, auth);
    }

    public boolean pagoPropio(Long pagoId, Authentication auth) {
        return propio(Recurso.PAGO, pagoId, auth);
    }

    private boolean propio(Recurso recurso, Long recursoId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        if (userId == null || recursoId == null) return false;
        return consulta.esDueno(recurso, recursoId, userId);
    }
}
