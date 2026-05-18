package com.agroruta.report.domain;

import java.time.LocalDateTime;
import java.util.List;

public record CropReport(
        LocalDateTime generatedAt,
        List<CropDetail> cropDetails
) {}