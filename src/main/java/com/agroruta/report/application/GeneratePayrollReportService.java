package com.agroruta.report.application;

import com.agroruta.report.application.ports.in.GeneratePayrollReportUseCase;
import com.agroruta.report.application.ports.out.PayrollQueryPort;
import com.agroruta.report.domain.PayrollReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GeneratePayrollReportService implements GeneratePayrollReportUseCase {

    private final PayrollQueryPort payrollQueryPort;

    @Override
    public PayrollReport generate() {
        return new PayrollReport(
                LocalDateTime.now(),
                payrollQueryPort.getTotalAccumulated(),
                payrollQueryPort.getDetailPerWorker()
        );
    }
}