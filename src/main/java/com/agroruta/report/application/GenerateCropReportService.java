package com.agroruta.report.application;

import com.agroruta.report.application.ports.in.GenerateCropReportUseCase;
import com.agroruta.report.application.ports.out.CropQueryPort;
import com.agroruta.report.domain.CropReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GenerateCropReportService implements GenerateCropReportUseCase {

    private final CropQueryPort cropQueryPort;

    @Override
    public CropReport generate() {
        return new CropReport(
                LocalDateTime.now(),
                cropQueryPort.getAllCropDetails()
        );
    }
}