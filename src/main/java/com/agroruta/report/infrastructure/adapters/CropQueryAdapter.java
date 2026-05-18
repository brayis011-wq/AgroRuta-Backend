package com.agroruta.report.infrastructure.adapters;

import com.agroruta.report.application.ports.out.CropQueryPort;
import com.agroruta.report.domain.CropDetail;
import com.agroruta.cultivo.infrastructure.persistence.JpaCosechaRepository;
import com.agroruta.cultivo.infrastructure.persistence.JpaSiembraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CropQueryAdapter implements CropQueryPort {

    private final JpaSiembraRepository jpaSiembraRepository;
    private final JpaCosechaRepository jpaCosechaRepository;

    @Override
    public List<CropDetail> getAllCropDetails() {
        return jpaSiembraRepository.findAll().stream()
                .map(siembra -> new CropDetail(
                        siembra.getId(),
                        siembra.getVariedad().name(),
                        siembra.getEstadoCultivo().name(),
                        siembra.getFechaSiembra(),
                        ChronoUnit.DAYS.between(siembra.getFechaSiembra(), LocalDate.now()),
                        jpaCosechaRepository.sumCantidadKgBySiembraId(siembra.getId()),
                        jpaCosechaRepository.countBySiembraId(siembra.getId())
                ))
                .toList();
    }
}