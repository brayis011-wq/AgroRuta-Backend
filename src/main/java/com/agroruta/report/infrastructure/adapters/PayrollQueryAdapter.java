package com.agroruta.report.infrastructure.adapters;

import com.agroruta.report.application.ports.out.PayrollQueryPort;
import com.agroruta.report.domain.WorkerPayrollDetail;
import com.agroruta.worker.infrastructure.persistence.JpaPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PayrollQueryAdapter implements PayrollQueryPort {

    private final JpaPagoRepository jpaPagoRepository;

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
}