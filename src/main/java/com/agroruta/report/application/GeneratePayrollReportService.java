package com.agroruta.report.application;

import com.agroruta.report.application.ports.in.GeneratePayrollReportUseCase;
import com.agroruta.report.application.ports.out.PayrollQueryPort;
import com.agroruta.report.domain.PayrollReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public PayrollReport generateFor(Long agricultorId) {
        if (agricultorId == null) {
            return new PayrollReport(LocalDateTime.now(), BigDecimal.ZERO, List.of());
        }
        return new PayrollReport(
                LocalDateTime.now(),
                payrollQueryPort.getTotalAccumulatedByAgricultor(agricultorId),
                payrollQueryPort.getDetailPerWorkerByAgricultor(agricultorId)
        );
    }
}