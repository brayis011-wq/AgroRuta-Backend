package com.agroruta.report.application.ports.in;

import com.agroruta.report.domain.PayrollReport;

public interface GeneratePayrollReportUseCase {
    PayrollReport generate();
}