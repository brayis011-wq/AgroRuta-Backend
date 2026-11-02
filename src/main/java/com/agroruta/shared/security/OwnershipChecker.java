package com.agroruta.shared.security;

import com.agroruta.crop.domain.FincaRepository;
import com.agroruta.crop.domain.LoteRepository;
import com.agroruta.crop.domain.SiembraRepository;
import com.agroruta.worker.domain.CargoRepository;
import com.agroruta.worker.domain.JornalRepository;
import com.agroruta.worker.domain.NominaRepository;
import com.agroruta.worker.domain.PagoRepository;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.domain.TrabajadorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Verifica que el usuario autenticado sea dueño del recurso solicitado.
 * Crop:   Finca (agricultorId) -> Lote (fincaId) -> Siembra (loteId).
 * Worker: Trabajador y Cargo (agricultorId); Jornal, Nómina -> Trabajador; Pago -> Nómina.
 * Se usa desde @PreAuthorize("@ownership.xxx(#id, authentication)").
 * Si el recurso no existe o no es del usuario, devuelve false (403).
 * El Administrador siempre tiene acceso.
 */
@Component("ownership")
public class OwnershipChecker {

    private final FincaRepository fincaRepository;
    private final LoteRepository loteRepository;
    private final SiembraRepository siembraRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final JornalRepository jornalRepository;
    private final NominaRepository nominaRepository;
    private final PagoRepository pagoRepository;
    private final CargoRepository cargoRepository;

    public OwnershipChecker(FincaRepository fincaRepository,
                            LoteRepository loteRepository,
                            SiembraRepository siembraRepository,
                            TrabajadorRepository trabajadorRepository,
                            JornalRepository jornalRepository,
                            NominaRepository nominaRepository,
                            PagoRepository pagoRepository,
                            CargoRepository cargoRepository) {
        this.fincaRepository = fincaRepository;
        this.loteRepository = loteRepository;
        this.siembraRepository = siembraRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.jornalRepository = jornalRepository;
        this.nominaRepository = nominaRepository;
        this.pagoRepository = pagoRepository;
        this.cargoRepository = cargoRepository;
    }

    /** El agricultorId recibido debe ser el del usuario autenticado. */
    public boolean esAgricultor(Long agricultorId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        return userId != null && userId.equals(agricultorId);
    }

    // ── crop ─────────────────────────────────────────────────────────────

    public boolean fincaPropia(Long fincaId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        if (userId == null || fincaId == null) return false;
        return fincaRepository.findById(fincaId)
                .map(f -> userId.equals(f.getAgricultorId()))
                .orElse(false);
    }

    public boolean lotePropio(Long loteId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        if (CurrentUser.id(auth) == null || loteId == null) return false;
        return loteRepository.findById(loteId)
                .map(l -> fincaPropia(l.getFincaId(), auth))
                .orElse(false);
    }

    public boolean siembraPropia(Long siembraId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        if (CurrentUser.id(auth) == null || siembraId == null) return false;
        return siembraRepository.findById(siembraId)
                .map(s -> lotePropio(s.getLoteId(), auth))
                .orElse(false);
    }

    // ── worker ───────────────────────────────────────────────────────────

    public boolean trabajadorPropio(Long trabajadorId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        if (userId == null || trabajadorId == null) return false;
        return trabajadorRepository.buscarPorId(trabajadorId)
                .map(t -> esDueno(t, userId))
                .orElse(false);
    }

    public boolean cargoPropio(Long cargoId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        if (userId == null || cargoId == null) return false;
        return cargoRepository.buscarPorId(cargoId)
                .map(c -> userId.equals(c.getAgricultorId()))
                .orElse(false);
    }

    public boolean jornalPropio(Long jornalId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        if (userId == null || jornalId == null) return false;
        return jornalRepository.buscarPorId(jornalId)
                .map(j -> esDueno(j.getTrabajador(), userId))
                .orElse(false);
    }

    public boolean nominaPropia(Long nominaId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        if (userId == null || nominaId == null) return false;
        return nominaRepository.buscarPorId(nominaId)
                .map(n -> esDueno(n.getTrabajador(), userId))
                .orElse(false);
    }

    public boolean pagoPropio(Long pagoId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long userId = CurrentUser.id(auth);
        if (userId == null || pagoId == null) return false;
        return pagoRepository.buscarPorId(pagoId)
                .map(p -> p.getNomina() != null && esDueno(p.getNomina().getTrabajador(), userId))
                .orElse(false);
    }

    private boolean esDueno(Trabajador trabajador, Long userId) {
        return trabajador != null && userId.equals(trabajador.getAgricultorId());
    }
}