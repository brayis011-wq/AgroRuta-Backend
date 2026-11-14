package com.agroruta.report.infrastructure.adapter;

import com.agroruta.report.application.ports.out.PayrollQueryPort;
import com.agroruta.report.domain.WorkerPayrollDetail;
import com.agroruta.report.domain.MonthlyPayment;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.ResumenNominas;
import com.agroruta.worker.application.ports.out.NominaRepository;
import com.agroruta.worker.infrastructure.persistence.JpaPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayrollQueryAdapter implements PayrollQueryPort {

    private final JpaPagoRepository jpaPagoRepository;
    private final NominaRepository nominaRepository;

    @Override
    public BigDecimal getTotalAccumulated() {
        return jpaPagoRepository.sumTotalMonto();
    }

    @Override
    public List<WorkerPayrollDetail> getDetailPerWorker() {
        return jpaPagoRepository.findDetailPerWorker();
    }

    @Override
    public BigDecimal getTotalAccumulatedByAgricultor(Long agricultorId) {
        BigDecimal total = jpaPagoRepository.sumMontoByAgricultorId(agricultorId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public List<WorkerPayrollDetail> getDetailPerWorkerByAgricultor(Long agricultorId) {
        return jpaPagoRepository.findDetailPerWorkerByAgricultorId(agricultorId);
    }

    @Override
    public List<MonthlyPayment> getMonthlyPayments(Long agricultorId, LocalDate desde) {
        // Todos los meses del rango, aunque no tengan pagos, para que la gráfica no salte meses
        Map<YearMonth, MonthlyPayment> porMes = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(desde); !m.isAfter(YearMonth.now()); m = m.plusMonths(1)) {
            porMes.put(m, new MonthlyPayment(m.toString(), BigDecimal.ZERO, 0));
        }
        // La base suma por mes: [año, mes, total, pagos]. Antes se traían todos los pagos para sumarlos aquí
        for (Object[] fila : jpaPagoRepository.totalesPorMes(agricultorId, desde)) {
            YearMonth mes = YearMonth.of(((Number) fila[0]).intValue(), ((Number) fila[1]).intValue());
            if (!porMes.containsKey(mes)) continue;   // un pago con fecha futura queda fuera de la gráfica
            porMes.put(mes, new MonthlyPayment(mes.toString(), decimal(fila[2]), ((Number) fila[3]).intValue()));
        }
        return new ArrayList<>(porMes.values());
    }

    @Override
    public PendingPayroll getPendingPayroll(Long agricultorId) {
        int n = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (ResumenNominas r : nominaRepository.resumenPorEstado(agricultorId, null)) {
            if (r.estado() == EstadoNomina.PENDIENTE || r.estado() == EstadoNomina.APROBADA) {
                n += (int) r.cantidad();
                total = total.add(r.total() != null ? r.total() : BigDecimal.ZERO);
            }
        }
        return new PendingPayroll(n, total);
    }

    private static BigDecimal decimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }
}
