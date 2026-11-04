package com.agroruta.report.application;

import com.agroruta.report.application.ports.in.GeneratePayrollReportUseCase;
import com.agroruta.report.application.ports.out.PayrollQueryPort;
import com.agroruta.report.domain.PayrollReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.agroruta.report.domain.WorkerPayrollDetail;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeneratePayrollReportService implements GeneratePayrollReportUseCase {

    private final PayrollQueryPort payrollQueryPort;

    /** Meses que muestra la gráfica de pagos (incluido el actual) */
    static final int MESES_SERIE = 6;

    @Override
    public PayrollReport generate() {
        return armar(null, payrollQueryPort.getTotalAccumulated(), payrollQueryPort.getDetailPerWorker());
    }

    @Override
    public PayrollReport generateFor(Long agricultorId) {
        if (agricultorId == null) {
            return new PayrollReport(LocalDateTime.now(), BigDecimal.ZERO, List.of());
        }
        return armar(agricultorId,
                payrollQueryPort.getTotalAccumulatedByAgricultor(agricultorId),
                payrollQueryPort.getDetailPerWorkerByAgricultor(agricultorId));
    }

    private PayrollReport armar(Long agricultorId, BigDecimal total, List<WorkerPayrollDetail> detalle) {
        LocalDate desde = LocalDate.now().withDayOfMonth(1).minusMonths(MESES_SERIE - 1L);
        PayrollQueryPort.PendingPayroll pendiente = payrollQueryPort.getPendingPayroll(agricultorId);
        // Los trabajadores con más pagado primero
        List<WorkerPayrollDetail> ordenado = detalle == null ? List.of() : detalle.stream()
                .sorted(Comparator.comparing(WorkerPayrollDetail::totalPaid,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return new PayrollReport(
                LocalDateTime.now(), total, ordenado,
                payrollQueryPort.getMonthlyPayments(agricultorId, desde),
                pendiente != null ? pendiente.total() : BigDecimal.ZERO,
                pendiente != null ? pendiente.nominas() : 0);
    }
}