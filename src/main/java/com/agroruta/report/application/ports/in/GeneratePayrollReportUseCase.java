package com.agroruta.report.application.ports.in;

import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.Periodo;

public interface GeneratePayrollReportUseCase {
    /** Reporte de toda la nómina (uso del Administrador). */
    PayrollReport generate();

    /** Reporte solo de los pagos de un agricultor. Si agricultorId es null, el reporte sale vacío. */
    PayrollReport generateFor(Long agricultorId);

    /**
     * Lo pagado dentro del periodo. {@code todos} = true es el reporte del Administrador (todos los
     * agricultores); si no, solo el de {@code agricultorId}. Lo pendiente por pagar no depende del periodo.
     */
    PayrollReport generateEn(Long agricultorId, boolean todos, Periodo periodo);
}
