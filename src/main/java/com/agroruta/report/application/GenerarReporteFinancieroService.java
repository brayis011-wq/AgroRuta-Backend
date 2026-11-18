package com.agroruta.report.application;

import com.agroruta.report.application.ports.in.GenerarReporteFinancieroUseCase;
import com.agroruta.report.application.ports.out.CostosQueryPort;
import com.agroruta.report.application.ports.out.CropQueryPort;
import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.ReporteFinanciero;
import com.agroruta.report.domain.TemporadaFinanzas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Junta cada temporada con sus costos, para los reportes de costos y de rentabilidad. */
@Service
@RequiredArgsConstructor
public class GenerarReporteFinancieroService implements GenerarReporteFinancieroUseCase {

    private final CropQueryPort cropQueryPort;
    private final CostosQueryPort costosQueryPort;

    @Transactional(readOnly = true)
    @Override
    public ReporteFinanciero generate() {
        return reporte(cropQueryPort.getAllCropDetails());
    }

    @Transactional(readOnly = true)
    @Override
    public ReporteFinanciero generateFor(Long agricultorId) {
        if (agricultorId == null) {
            return new ReporteFinanciero(LocalDateTime.now(), List.of());
        }
        return reporte(cropQueryPort.getCropDetailsByAgricultor(agricultorId));
    }

    private ReporteFinanciero reporte(List<CropDetail> siembras) {
        return new ReporteFinanciero(LocalDateTime.now(), siembras.stream()
                .map(s -> new TemporadaFinanzas(s, costosQueryPort.costosDe(s.siembraId()).orElse(null)))
                .toList());
    }
}
