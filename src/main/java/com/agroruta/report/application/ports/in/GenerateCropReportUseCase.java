package com.agroruta.report.application.ports.in;

import com.agroruta.report.domain.CropReport;

public interface GenerateCropReportUseCase {
    CropReport generate();
}