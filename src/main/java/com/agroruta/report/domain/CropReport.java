package com.agroruta.report.domain;

import java.time.LocalDateTime;
import java.util.List;

public record CropReport(
        LocalDateTime generatedAt,
        List<CropDetail> cropDetails
) {
    /** Solo las temporadas que estuvieron en el campo durante el periodo */
    public CropReport en(Periodo periodo) {
        if (periodo == null || periodo.esTodo() || cropDetails == null) return this;
        return new CropReport(generatedAt, cropDetails.stream()
                .filter(c -> periodo.seCruzaCon(c.fechaSiembra(), c.fechaFin()))
                .toList());
    }
}
