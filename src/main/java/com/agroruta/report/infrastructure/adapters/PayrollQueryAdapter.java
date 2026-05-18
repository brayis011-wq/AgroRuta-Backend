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
}