package com.agroruta.report.application;

import com.agroruta.report.application.ports.in.GeneratePayrollReportUseCase;
import com.agroruta.report.application.ports.out.PayrollQueryPort;
import com.agroruta.report.domain.MonthlyPayment;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.Periodo;
import com.agroruta.report.domain.WorkerPayrollDetail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        return armar(
                agricultorId,
                payrollQueryPort.getTotalAccumulatedByAgricultor(agricultorId),
                payrollQueryPort.getDetailPerWorkerByAgricultor(agricultorId));
    }

    /** Con periodos largos la gráfica muestra solo los últimos meses (no cabe una barra por mes de varios años) */
    static final int MESES_SERIE_MAXIMO = 24;

    @Override
    public PayrollReport generateEn(Long agricultorId, boolean todos, Periodo periodo) {
        if (periodo == null || periodo.esTodo()) {
            return todos ? generate() : generateFor(agricultorId);
        }
        if (!todos && agricultorId == null) {
            return new PayrollReport(LocalDateTime.now(), BigDecimal.ZERO, List.of());
        }
        Long dueno = todos ? null : agricultorId;
        return conDetalle(
                payrollQueryPort.getTotalPaid(dueno, periodo),
                payrollQueryPort.getDetailPerWorker(dueno, periodo),
                payrollQueryPort.getMonthlyPayments(dueno, inicioSerie(periodo), finSerie(periodo)),
                payrollQueryPort.getPendingPayroll(dueno));
    }

    /** La gráfica va del primer mes del periodo al último, sin pasar de hoy ni de {@link #MESES_SERIE_MAXIMO} meses */
    static LocalDate finSerie(Periodo periodo) {
        LocalDate hoy = LocalDate.now();
        return periodo.hasta() == null || periodo.hasta().isAfter(hoy) ? hoy : periodo.hasta();
    }

    static LocalDate inicioSerie(Periodo periodo) {
        LocalDate fin = finSerie(periodo);
        LocalDate tope = fin.withDayOfMonth(1).minusMonths(MESES_SERIE_MAXIMO - 1L);
        LocalDate desde =
                periodo.desde() == null ? fin.withDayOfMonth(1).minusMonths(MESES_SERIE - 1L) : periodo.desde();
        if (desde.isAfter(fin)) desde = fin;
        return desde.isBefore(tope) ? tope : desde;
    }

    private PayrollReport armar(Long agricultorId, BigDecimal total, List<WorkerPayrollDetail> detalle) {
        LocalDate desde = LocalDate.now().withDayOfMonth(1).minusMonths(MESES_SERIE - 1L);
        return conDetalle(
                total,
                detalle,
                payrollQueryPort.getMonthlyPayments(agricultorId, desde),
                payrollQueryPort.getPendingPayroll(agricultorId));
    }

    private PayrollReport conDetalle(
            BigDecimal total,
            List<WorkerPayrollDetail> detalle,
            List<MonthlyPayment> serie,
            PayrollQueryPort.PendingPayroll pendiente) {
        // Los trabajadores con más pagado primero
        List<WorkerPayrollDetail> ordenado = detalle == null
                ? List.of()
                : detalle.stream()
                        .sorted(Comparator.comparing(
                                WorkerPayrollDetail::totalPaid, Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();
        return new PayrollReport(
                LocalDateTime.now(),
                total,
                ordenado,
                serie,
                pendiente != null ? pendiente.total() : BigDecimal.ZERO,
                pendiente != null ? pendiente.nominas() : 0);
    }
}
