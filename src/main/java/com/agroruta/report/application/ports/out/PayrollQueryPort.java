package com.agroruta.report.application.ports.out;

import com.agroruta.report.domain.WorkerPayrollDetail;
import java.math.BigDecimal;
import java.util.List;

public interface PayrollQueryPort {
    BigDecimal getTotalAccumulated();
    List<WorkerPayrollDetail> getDetailPerWorker();
}