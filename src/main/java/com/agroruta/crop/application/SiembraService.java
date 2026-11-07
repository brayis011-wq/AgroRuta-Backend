package com.agroruta.crop.application;

import com.agroruta.shared.util.EnumParser;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.application.ports.in.LoteUseCase;
import com.agroruta.crop.domain.*;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Temporadas de cultivo. Un lote guarda todas sus siembras, pero solo una puede estar en curso.
 * El estado del lote se mantiene solo: EN_CULTIVO al sembrar, DISPONIBLE (o EN_DESCANSO) al terminar.
 * Cada cambio de etapa queda registrado con su fecha.
 */
@Service
public class SiembraService implements SiembraUseCase {

    private final SiembraRepository siembraRepository;
    private final LoteUseCase loteUseCase;
    private final CatalogoCultivoRepository catalogo;
    private final EtapaSiembraRepository etapaRepository;

    public SiembraService(SiembraRepository siembraRepository, LoteUseCase loteUseCase,
                          CatalogoCultivoRepository catalogo, EtapaSiembraRepository etapaRepository) {
        this.siembraRepository = siembraRepository;
        this.loteUseCase = loteUseCase;
        this.catalogo = catalogo;
        this.etapaRepository = etapaRepository;
    }

    @Override
    public Siembra registrarSiembra(LocalDate fechaSiembra, Integer cantidadPlantas,
                                    Long variedadId, Long loteId) {

        loteUseCase.buscarLotePorId(loteId);

        siembraRepository.findActivaByLoteId(loteId).ifPresent(activa -> {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "El lote ya tiene una siembra en curso (" + activa.nombreCompleto()
                            + "). Finalízala antes de sembrar de nuevo.");
        });

        VariedadCultivo variedad = variedadDisponible(variedadId);

        Siembra siembra = siembraRepository.save(
                new Siembra(null, fechaSiembra, cantidadPlantas, variedad, loteId));
        registrarEtapa(siembra, EstadoCultivo.GERMINACION, fechaSiembra);
        loteUseCase.ocuparLote(loteId);
        return siembra;
    }

    @Override
    public Siembra buscarSiembraPorId(Long id) {
        return siembraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Siembra no encontrada con ID: " + id));
    }

    @Override
    public Siembra buscarSiembraPorLote(Long loteId) {
        return siembraRepository.findActivaByLoteId(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay siembra activa en el lote: " + loteId));
    }

    @Override
    public List<Siembra> listarHistorialPorLote(Long loteId) {
        loteUseCase.buscarLotePorId(loteId);
        return siembraRepository.findHistorialByLoteId(loteId);
    }

    @Override
    public Siembra actualizarSiembra(Long id, LocalDate fechaSiembra, Integer cantidadPlantas, Long variedadId) {
        Siembra siembra = buscarSiembraPorId(id);

        if (fechaSiembra != null) {
            List<EtapaSiembra> etapas = etapaRepository.findBySiembraId(id);
            etapas.stream()
                    .filter(e -> e.getEtapa() != EstadoCultivo.GERMINACION && e.getFecha() != null)
                    .filter(e -> e.getFecha().isBefore(fechaSiembra))
                    .findFirst()
                    .ifPresent(e -> {
                        throw new BusinessException("La fecha de siembra no puede ser posterior al cambio a "
                                + e.getEtapa().name().toLowerCase() + " (" + e.getFecha() + ").");
                    });
            if (!fechaSiembra.equals(siembra.getFechaSiembra())) {
                etapas.stream()
                        .filter(e -> e.getEtapa() == EstadoCultivo.GERMINACION)
                        .findFirst()
                        .ifPresent(e -> {
                            e.setFecha(fechaSiembra);
                            etapaRepository.save(e);
                        });
            }
            siembra.setFechaSiembra(fechaSiembra);
        }
        if (cantidadPlantas != null) siembra.setCantidadPlantas(cantidadPlantas);
        if (variedadId != null && !variedadId.equals(siembra.getVariedadId())) {
            siembra.cambiarVariedad(variedadDisponible(variedadId));
        }
        return siembraRepository.save(siembra);
    }

    @Override
    public Siembra avanzarEtapa(Long siembraId, LocalDate fecha) {
        Siembra siembra = buscarSiembraPorId(siembraId);
        if (!siembra.estaActiva()) throw new BusinessException("El cultivo ya está finalizado.");

        LocalDate dia = validarFecha(siembra, fecha);
        siembra.avanzarEtapa();
        if (!siembra.estaActiva()) siembra.setFechaFin(dia);

        Siembra guardada = siembraRepository.save(siembra);
        registrarEtapa(guardada, guardada.getEstadoCultivo(), dia);
        if (!guardada.estaActiva()) loteUseCase.liberarLote(guardada.getLoteId(), EstadoLote.DISPONIBLE);
        return guardada;
    }

    @Override
    public Siembra retrocederEtapa(Long siembraId) {
        Siembra siembra = buscarSiembraPorId(siembraId);
        if (siembra.getEstadoCultivo() == EstadoCultivo.GERMINACION) {
            throw new BusinessException("El cultivo ya está en la primera etapa.");
        }
        boolean reabre = !siembra.estaActiva();
        if (reabre && siembraRepository.existsActivaByLoteId(siembra.getLoteId())) {
            throw new BusinessException(
                    "No se puede reabrir esta siembra: el lote ya tiene otra siembra en curso.");
        }

        EstadoCultivo etapaQueSeDeshace = siembra.getEstadoCultivo();
        siembra.retrocederEtapa();
        Siembra guardada = siembraRepository.save(siembra);

        etapaRepository.findUltima(siembraId)
                .filter(e -> e.getEtapa() == etapaQueSeDeshace)
                .ifPresent(e -> etapaRepository.deleteById(e.getId()));
        if (reabre) loteUseCase.ocuparLote(guardada.getLoteId());
        return guardada;
    }

    @Override
    public Siembra finalizarSiembra(Long siembraId, LocalDate fecha, boolean dejarLoteEnDescanso) {
        Siembra siembra = buscarSiembraPorId(siembraId);
        if (!siembra.estaActiva()) throw new BusinessException("El cultivo ya está finalizado.");

        LocalDate dia = validarFecha(siembra, fecha);
        siembra.finalizar(dia);
        Siembra guardada = siembraRepository.save(siembra);
        registrarEtapa(guardada, EstadoCultivo.FINALIZADO, dia);
        loteUseCase.liberarLote(guardada.getLoteId(),
                dejarLoteEnDescanso ? EstadoLote.EN_DESCANSO : EstadoLote.DISPONIBLE);
        return guardada;
    }

    @Override
    public List<EtapaSiembra> listarEtapas(Long siembraId) {
        buscarSiembraPorId(siembraId);
        return etapaRepository.findBySiembraId(siembraId);
    }

    @Override
    public List<Siembra> listarSiembrasPorEstado(String estado) {
        EstadoCultivo estadoCultivo = EnumParser.parse(EstadoCultivo.class, estado, "estado");
        return siembraRepository.findByEstadoCultivo(estadoCultivo);
    }

    /**
     * Borrado lógico: la siembra deja de verse en el lote, pero sus actividades, fumigaciones,
     * cosechas y jornales se conservan y siguen en los reportes de cultivos y de costos.
     * Si estaba en curso, se cierra hoy (queda la etapa FINALIZADO) y el lote queda libre.
     */
    @Override
    @Transactional
    public void eliminarSiembra(Long id) {
        Siembra siembra = buscarSiembraPorId(id);
        if (siembra.estaEliminada()) return;
        boolean estabaActiva = siembra.estaActiva();
        LocalDate hoy = LocalDate.now();
        siembra.eliminar(hoy, LocalDateTime.now());
        Siembra guardada = siembraRepository.save(siembra);
        if (estabaActiva) {
            registrarEtapa(guardada, EstadoCultivo.FINALIZADO, hoy);
            loteUseCase.liberarLote(guardada.getLoteId(), EstadoLote.DISPONIBLE);
        }
    }

    // ── apoyo ─────────────────────────────────────────────────────────────

    private VariedadCultivo variedadDisponible(Long variedadId) {
        VariedadCultivo variedad = catalogo.buscarVariedad(variedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Variedad no encontrada con ID: " + variedadId));
        if (!variedad.isActivo() || !variedad.isTipoCultivoActivo()) {
            throw new BusinessException(
                    "La variedad " + variedad.getNombre() + " de " + variedad.getTipoCultivoNombre()
                            + " ya no está disponible para nuevas siembras.");
        }
        return variedad;
    }

    /** La fecha de un cambio de etapa no puede ser futura ni anterior al cambio anterior. */
    private LocalDate validarFecha(Siembra siembra, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        if (dia.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha del cambio de etapa no puede ser futura.");
        }
        LocalDate anterior = etapaRepository.findUltima(siembra.getId())
                .map(EtapaSiembra::getFecha)
                .filter(Objects::nonNull)
                .orElse(siembra.getFechaSiembra());
        if (anterior != null && dia.isBefore(anterior)) {
            throw new BusinessException("La fecha no puede ser anterior a la etapa anterior (" + anterior + ").");
        }
        return dia;
    }

    private void registrarEtapa(Siembra siembra, EstadoCultivo etapa, LocalDate fecha) {
        if (siembra == null || siembra.getId() == null) return;
        etapaRepository.save(new EtapaSiembra(null, siembra.getId(), etapa, fecha));
    }
}
