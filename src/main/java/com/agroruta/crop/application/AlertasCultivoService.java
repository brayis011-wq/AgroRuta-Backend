package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.AlertasCultivoUseCase;
import com.agroruta.crop.domain.ActividadCultivoRepository;
import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.AlertasCultivo;
import com.agroruta.crop.domain.CalendarioCultivo.DatosSiembra;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.domain.FincaRepository;
import com.agroruta.crop.domain.FumigacionRepository;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.domain.LoteRepository;
import com.agroruta.crop.domain.SiembraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AlertasCultivoService implements AlertasCultivoUseCase {

    private final FincaRepository fincaRepository;
    private final LoteRepository loteRepository;
    private final SiembraRepository siembraRepository;
    private final ActividadCultivoRepository actividadRepository;
    private final FumigacionRepository fumigacionRepository;

    public AlertasCultivoService(FincaRepository fincaRepository, LoteRepository loteRepository,
                                 SiembraRepository siembraRepository, ActividadCultivoRepository actividadRepository,
                                 FumigacionRepository fumigacionRepository) {
        this.fincaRepository = fincaRepository;
        this.loteRepository = loteRepository;
        this.siembraRepository = siembraRepository;
        this.actividadRepository = actividadRepository;
        this.fumigacionRepository = fumigacionRepository;
    }

    @Override
    public List<AlertaCultivo> alertasDelDia(Long agricultorId, LocalDate hoy) {
        List<AlertaCultivo> alertas = new ArrayList<>();
        if (agricultorId == null) return alertas;
        for (Finca finca : fincaRepository.findByAgricultorId(agricultorId)) {
            for (Lote lote : loteRepository.findByFincaId(finca.getId())) {
                siembraRepository.findActivaByLoteId(lote.getId()).ifPresent(s -> {
                    DatosSiembra datos = new DatosSiembra(s, lote.getId(), lote.getNombre(), List.of(),
                            actividadRepository.findBySiembraId(s.getId()),
                            fumigacionRepository.findBySiembraId(s.getId()), List.of());
                    alertas.addAll(AlertasCultivo.delDia(datos, finca.getNombre(), hoy));
                });
            }
        }
        return alertas;
    }
}
