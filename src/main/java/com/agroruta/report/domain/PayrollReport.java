package com.agroruta.report.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PayrollReport(
        LocalDateTime generatedAt,
        BigDecimal totalAccumulated,
        List<WorkerPayrollDetail> workerDetails
) {}