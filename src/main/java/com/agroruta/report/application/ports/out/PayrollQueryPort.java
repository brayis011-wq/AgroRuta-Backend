package com.agroruta.report.application.ports.out;

import com.agroruta.report.domain.MonthlyPayment;
import java.time.LocalDate;
import com.agroruta.report.domain.WorkerPayrollDetail;
import java.math.BigDecimal;
import java.util.List;

public interface PayrollQueryPort {
    BigDecimal getTotalAccumulated();
    List<WorkerPayrollDetail> getDetailPerWorker();
    BigDecimal getTotalAccumulatedByAgricultor(Long agricultorId);
    List<WorkerPayrollDetail> getDetailPerWorkerByAgricultor(Long agricultorId);

    /** Pagos agrupados por mes desde {@code desde} (inclusive). agricultorId null = todos. */
    List<MonthlyPayment> getMonthlyPayments(Long agricultorId, LocalDate desde);

    /** Nóminas pendientes o aprobadas que aún no se pagan. agricultorId null = todos. */
    PendingPayroll getPendingPayroll(Long agricultorId);

    record PendingPayroll(int nominas, BigDecimal total) {}
}