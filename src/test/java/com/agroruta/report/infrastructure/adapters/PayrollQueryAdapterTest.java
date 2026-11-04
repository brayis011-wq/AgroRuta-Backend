package com.agroruta.report.infrastructure.adapters;

import com.agroruta.report.application.ports.out.PayrollQueryPort.PendingPayroll;
import com.agroruta.report.domain.MonthlyPayment;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.NominaRepository;
import com.agroruta.worker.infrastructure.persistence.JpaPagoRepository;
import com.agroruta.worker.infrastructure.persistence.PagoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayrollQueryAdapter - datos para el reporte")
class PayrollQueryAdapterTest {

    @Mock private JpaPagoRepository jpaPagoRepository;
    @Mock private NominaRepository nominaRepository;
    @InjectMocks private PayrollQueryAdapter adapter;

    private static PagoEntity pago(LocalDate fecha, String monto) {
        PagoEntity p = new PagoEntity();
        p.setFechaPago(fecha);
        p.setMonto(new BigDecimal(monto));
        return p;
    }

    private static Nomina nomina(EstadoNomina estado, String valor) {
        Nomina n = new Nomina();
        n.setEstado(estado);
        n.setValorTotal(new BigDecimal(valor));
        return n;
    }

    @Test
    @DisplayName("agrupa los pagos por mes e incluye los meses sin pagos")
    void agrupaPorMes() {
        YearMonth actual = YearMonth.now();
        LocalDate desde = actual.minusMonths(2).atDay(1);
        when(jpaPagoRepository.findByAgricultorId(7L)).thenReturn(List.of(
                pago(actual.atDay(2), "100"),
                pago(actual.atDay(5), "50"),
                pago(actual.minusMonths(2).atDay(10), "30"),
                pago(desde.minusDays(1), "999")));   // antes del rango: no cuenta

        List<MonthlyPayment> serie = adapter.getMonthlyPayments(7L, desde);

        assertThat(serie).extracting(MonthlyPayment::mes)
                .containsExactly(actual.minusMonths(2).toString(), actual.minusMonths(1).toString(), actual.toString());
        assertThat(serie.get(0).total()).isEqualByComparingTo("30");
        assertThat(serie.get(1).total()).isEqualByComparingTo("0");
        assertThat(serie.get(2).total()).isEqualByComparingTo("150");
        assertThat(serie.get(2).pagos()).isEqualTo(2);
    }

    @Test
    @DisplayName("sin agricultor usa todos los pagos")
    void todosLosPagos() {
        when(jpaPagoRepository.findAll()).thenReturn(List.of());
        adapter.getMonthlyPayments(null, LocalDate.now().withDayOfMonth(1));
        verify(jpaPagoRepository).findAll();
    }

    @Test
    @DisplayName("suma solo las nóminas pendientes y aprobadas")
    void pendientePorPagar() {
        when(nominaRepository.listarPorAgricultor(7L)).thenReturn(List.of(
                nomina(EstadoNomina.PENDIENTE, "100"),
                nomina(EstadoNomina.APROBADA, "40"),
                nomina(EstadoNomina.PAGADA, "999"),
                nomina(EstadoNomina.ANULADA, "999")));

        PendingPayroll p = adapter.getPendingPayroll(7L);

        assertThat(p.nominas()).isEqualTo(2);
        assertThat(p.total()).isEqualByComparingTo("140");
    }
}
