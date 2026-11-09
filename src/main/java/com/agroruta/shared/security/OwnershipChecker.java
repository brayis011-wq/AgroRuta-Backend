package com.agroruta.shared.security;

import com.agroruta.crop.application.ports.out.ActividadCultivoRepository;
import com.agroruta.crop.application.ports.out.CosechaRepository;
import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.crop.application.ports.out.FumigacionRepository;
import com.agroruta.crop.application.ports.out.LoteRepository;
import com.agroruta.crop.application.ports.out.SiembraRepository;
import com.agroruta.worker.application.ports.out.LaborRepository;
import com.agroruta.worker.application.ports.out.CargoRepository;
import com.agroruta.worker.application.ports.out.JornalRepository;
import com.agroruta.worker.application.ports.out.NominaRepository;
import com.agroruta.worker.application.ports.out.PagoRepository;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Verifica que el usuario autenticado sea dueño del recurso solicitado.
 * Crop:   Finca (agricultorId) -> Lote (fincaId) -> Siembra (loteId) -> Cosecha, Fumigación, Actividad.
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
    private final CosechaRepository cosechaRepository;
    private final FumigacionRepository fumigacionRepository;
    private final ActividadCultivoRepository actividadCultivoRepository;
    private final LaborRepository laborRepository;

    public OwnershipChecker(FincaRepository fincaRepository,
                            LoteRepository loteRepository,
                            SiembraRepository siembraRepository,
                            TrabajadorRepository trabajadorRepository,
                            JornalRepository jornalRepository,
                            NominaRepository nominaRepository,
                            PagoRepository pagoRepository,
                            CargoRepository cargoRepository,
                            CosechaRepository cosechaRepository,
                            FumigacionRepository fumigacionRepository,
                            ActividadCultivoRepository actividadCultivoRepository,
                            LaborRepository laborRepository) {
        this.fincaRepository = fincaRepository;
        this.loteRepository = loteRepository;
        this.siembraRepository = siembraRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.jornalRepository = jornalRepository;
        this.nominaRepository = nominaRepository;
        this.pagoRepository = pagoRepository;
        this.cargoRepository = cargoRepository;
        this.cosechaRepository = cosechaRepository;
        this.fumigacionRepository = fumigacionRepository;
        this.actividadCultivoRepository = actividadCultivoRepository;
        this.laborRepository = laborRepository;
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

    public boolean cosechaPropia(Long cosechaId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        if (CurrentUser.id(auth) == null || cosechaId == null) return false;
        return cosechaRepository.findById(cosechaId)
                .map(c -> siembraPropia(c.getSiembraId(), auth))
                .orElse(false);
    }

    public boolean fumigacionPropia(Long fumigacionId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        if (CurrentUser.id(auth) == null || fumigacionId == null) return false;
        return fumigacionRepository.findById(fumigacionId)
                .map(f -> siembraPropia(f.getSiembraId(), auth))
                .orElse(false);
    }

    public boolean actividadPropia(Long actividadId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        if (CurrentUser.id(auth) == null || actividadId == null) return false;
        return actividadCultivoRepository.findById(actividadId)
                .map(a -> siembraPropia(a.getSiembraId(), auth))
                .orElse(false);
    }

    // ── worker ───────────────────────────────────────────────────────────

    /** Labor de los jornales: el agricultor solo modifica las suyas; las comunes, solo el Administrador. */
    public boolean laborPropia(Long actividadId, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return true;
        Long usuarioId = CurrentUser.id(auth);
        if (usuarioId == null || actividadId == null) return false;
        return laborRepository.buscarPorId(actividadId)
                .map(a -> !a.esComun() && usuarioId.equals(a.getAgricultorId()))
                .orElse(false);
    }

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