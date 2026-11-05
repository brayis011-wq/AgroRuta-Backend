package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/** Arma el resumen de una siembra: rendimiento, cosechas por semana, carencia y reentrada. */
@Service
public class ResumenSiembraService {

    /** Las fechas de la app son de Colombia */
    static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final SiembraUseCase siembraUseCase;
    private final SiembraRepository siembraRepository;
    private final LoteRepository loteRepository;
    private final CosechaRepository cosechaRepository;
    private final FumigacionRepository fumigacionRepository;

    public ResumenSiembraService(SiembraUseCase siembraUseCase, SiembraRepository siembraRepository,
                                 LoteRepository loteRepository, CosechaRepository cosechaRepository,
                                 FumigacionRepository fumigacionRepository) {
        this.siembraUseCase = siembraUseCase;
        this.siembraRepository = siembraRepository;
        this.loteRepository = loteRepository;
        this.cosechaRepository = cosechaRepository;
        this.fumigacionRepository = fumigacionRepository;
    }

    public ResumenSiembra resumen(Long siembraId) {
        Siembra siembra = siembraUseCase.buscarSiembraPorId(siembraId);
        Double area = loteRepository.findById(siembra.getLoteId()).map(Lote::getArea).orElse(null);
        return CalculadoraResumenSiembra.calcular(
                siembra, area,
                cosechaRepository.findBySiembraId(siembraId),
                fumigacionRepository.findBySiembraId(siembraId),
                siembraRepository.findHistorialByLoteId(siembra.getLoteId()),
                cosechaRepository::totalKgBySiembraId,
                LocalDate.now(ZONA));
    }
}
