package com.agroruta.crop.infrastructure.adapter;

import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.domain.FincaRepository;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.domain.LoteRepository;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.SiembraRepository;
import com.agroruta.worker.application.ports.out.CultivoPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Expone las siembras al módulo de trabajadores (para registrar jornales). */
@Component
public class CultivoPortAdapter implements CultivoPort {

    private final FincaRepository fincaRepository;
    private final LoteRepository loteRepository;
    private final SiembraRepository siembraRepository;

    public CultivoPortAdapter(FincaRepository fincaRepository,
                              LoteRepository loteRepository,
                              SiembraRepository siembraRepository) {
        this.fincaRepository = fincaRepository;
        this.loteRepository = loteRepository;
        this.siembraRepository = siembraRepository;
    }

    @Override
    public Optional<CultivoInfo> buscar(Long cultivoId) {
        if (cultivoId == null) return Optional.empty();
        return siembraRepository.findById(cultivoId).flatMap(s ->
                loteRepository.findById(s.getLoteId()).flatMap(l ->
                        fincaRepository.findById(l.getFincaId()).map(f -> info(s, l, f))));
    }

    @Override
    public List<CultivoInfo> activosDelAgricultor(Long agricultorId) {
        List<CultivoInfo> cultivos = new ArrayList<>();
        if (agricultorId == null) return cultivos;
        for (Finca finca : fincaRepository.findByAgricultorId(agricultorId)) {
            for (Lote lote : loteRepository.findByFincaId(finca.getId())) {
                siembraRepository.findByLoteId(lote.getId())
                        .filter(s -> s.getEstadoCultivo() != EstadoCultivo.FINALIZADO)
                        .ifPresent(s -> cultivos.add(info(s, lote, finca)));
            }
        }
        return cultivos;
    }

    private static CultivoInfo info(Siembra s, Lote l, Finca f) {
        return new CultivoInfo(
                s.getId(), f.getAgricultorId(),
                f.getId(), f.getNombre(),
                l.getId(), l.getNombre(),
                s.getTipoCultivo(), s.getVariedad(),
                s.getEstadoCultivo() != null ? s.getEstadoCultivo().name() : null);
    }
}
