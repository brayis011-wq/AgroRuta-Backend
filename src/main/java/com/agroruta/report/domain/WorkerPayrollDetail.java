package com.agroruta.report.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkerPayrollDetail(
        Long workerId,
        String workerName,
        Integer totalPagos,
        BigDecimal totalPaid,
        LocalDate periodoInicio,
        LocalDate periodoFin,
        Integer totalJornales
) {}