package com.agroruta.report.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PayrollReport(
        LocalDateTime generatedAt,
        BigDecimal totalAccumulated,
        List<WorkerPayrollDetail> workerDetails,
        /** Últimos meses, del más antiguo al más reciente (incluye meses en 0) */
        List<MonthlyPayment> pagosPorMes,
        /** Suma de nóminas pendientes y aprobadas que aún no se pagan */
        BigDecimal totalPendiente,
        int nominasPendientes
) {
    public PayrollReport {
        totalAccumulated = totalAccumulated != null ? totalAccumulated : BigDecimal.ZERO;
        workerDetails = workerDetails != null ? workerDetails : List.of();
        pagosPorMes = pagosPorMes != null ? pagosPorMes : List.of();
        totalPendiente = totalPendiente != null ? totalPendiente : BigDecimal.ZERO;
    }

    /** Solo los datos básicos (sin serie mensual ni pendientes). */
    public PayrollReport(LocalDateTime generatedAt, BigDecimal totalAccumulated, List<WorkerPayrollDetail> workerDetails) {
        this(generatedAt, totalAccumulated, workerDetails, List.of(), BigDecimal.ZERO, 0);
    }
}
