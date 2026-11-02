package com.agroruta.report.application.ports.in;

import com.agroruta.report.domain.PayrollReport;

public interface GeneratePayrollReportUseCase {
    /** Reporte de toda la nómina (uso del Administrador). */
    PayrollReport generate();

    /** Reporte solo de los pagos de un agricultor. Si agricultorId es null, el reporte sale vacío. */
    PayrollReport generateFor(Long agricultorId);
}