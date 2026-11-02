package com.agroruta.report.application.ports.in;

import com.agroruta.report.domain.CropReport;

public interface GenerateCropReportUseCase {
    /** Reporte de todos los cultivos (uso del Administrador). */
    CropReport generate();

    /** Reporte solo de los cultivos de un agricultor. Si agricultorId es null, el reporte sale vacío. */
    CropReport generateFor(Long agricultorId);
}