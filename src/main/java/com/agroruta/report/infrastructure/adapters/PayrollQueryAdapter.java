package com.agroruta.report.infrastructure.adapters;

import com.agroruta.report.application.ports.out.PayrollQueryPort;
import com.agroruta.report.domain.WorkerPayrollDetail;
import com.agroruta.report.domain.MonthlyPayment;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.NominaRepository;
import com.agroruta.worker.infrastructure.persistence.JpaPagoRepository;
import com.agroruta.worker.infrastructure.persistence.PagoEntity;
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
        List<PagoEntity> pagos = agricultorId == null
                ? jpaPagoRepository.findAll()
                : jpaPagoRepository.findByAgricultorId(agricultorId);

        // Todos los meses del rango, aunque no tengan pagos, para que la gráfica no salte meses
        Map<YearMonth, BigDecimal[]> porMes = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(desde); !m.isAfter(YearMonth.now()); m = m.plusMonths(1)) {
            porMes.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (PagoEntity p : pagos) {
            if (p.getFechaPago() == null || p.getFechaPago().isBefore(desde)) continue;
            BigDecimal[] acc = porMes.get(YearMonth.from(p.getFechaPago()));
            if (acc == null) continue;
            acc[0] = acc[0].add(p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO);
            acc[1] = acc[1].add(BigDecimal.ONE);
        }
        List<MonthlyPayment> serie = new ArrayList<>();
        porMes.forEach((mes, acc) -> serie.add(new MonthlyPayment(mes.toString(), acc[0], acc[1].intValue())));
        return serie;
    }

    @Override
    public PendingPayroll getPendingPayroll(Long agricultorId) {
        List<Nomina> nominas = agricultorId == null
                ? nominaRepository.listarTodas()
                : nominaRepository.listarPorAgricultor(agricultorId);
        int n = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Nomina nom : nominas) {
            if (nom.getEstado() == EstadoNomina.PENDIENTE || nom.getEstado() == EstadoNomina.APROBADA) {
                n++;
                total = total.add(nom.getValorTotal() != null ? nom.getValorTotal() : BigDecimal.ZERO);
            }
        }
        return new PendingPayroll(n, total);
    }
}
