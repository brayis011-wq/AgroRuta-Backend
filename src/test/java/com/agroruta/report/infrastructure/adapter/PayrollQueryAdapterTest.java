package com.agroruta.report.infrastructure.adapter;

import com.agroruta.report.application.ports.out.PayrollQueryPort.PendingPayroll;
import com.agroruta.report.domain.MonthlyPayment;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.ResumenNominas;
import com.agroruta.worker.application.ports.out.NominaRepository;
import com.agroruta.worker.infrastructure.persistence.JpaPagoRepository;
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

    /** Fila como la devuelve la base: [año, mes, total, pagos] */
    private static Object[] mes(YearMonth m, String total, long pagos) {
        return new Object[]{m.getYear(), m.getMonthValue(), new BigDecimal(total), pagos};
    }

    @Test
    @DisplayName("arma la serie con los totales por mes e incluye los meses sin pagos")
    void agrupaPorMes() {
        YearMonth actual = YearMonth.now();
        LocalDate desde = actual.minusMonths(2).atDay(1);
        when(jpaPagoRepository.totalesPorMes(7L, desde)).thenReturn(List.of(
                mes(actual, "150", 2),
                mes(actual.minusMonths(2), "30", 1)));

        List<MonthlyPayment> serie = adapter.getMonthlyPayments(7L, desde);

        assertThat(serie).extracting(MonthlyPayment::mes)
                .containsExactly(actual.minusMonths(2).toString(), actual.minusMonths(1).toString(), actual.toString());
        assertThat(serie.get(0).total()).isEqualByComparingTo("30");
        assertThat(serie.get(0).pagos()).isEqualTo(1);
        assertThat(serie.get(1).total()).isEqualByComparingTo("0");
        assertThat(serie.get(1).pagos()).isZero();
        assertThat(serie.get(2).total()).isEqualByComparingTo("150");
        assertThat(serie.get(2).pagos()).isEqualTo(2);
    }

    @Test
    @DisplayName("un pago con fecha futura no agrega meses a la serie")
    void ignoraMesesFueraDelRango() {
        YearMonth actual = YearMonth.now();
        LocalDate desde = actual.atDay(1);
        when(jpaPagoRepository.totalesPorMes(7L, desde)).thenReturn(List.<Object[]>of(
                mes(actual.plusMonths(1), "999", 1)));

        List<MonthlyPayment> serie = adapter.getMonthlyPayments(7L, desde);

        assertThat(serie).extracting(MonthlyPayment::mes).containsExactly(actual.toString());
        assertThat(serie.get(0).total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("sin agricultor pide los totales de todos, sin traer los pagos uno por uno")
    void todosLosPagos() {
        LocalDate desde = LocalDate.now().withDayOfMonth(1);
        when(jpaPagoRepository.totalesPorMes(null, desde)).thenReturn(List.of());

        adapter.getMonthlyPayments(null, desde);

        verify(jpaPagoRepository).totalesPorMes(null, desde);
        verify(jpaPagoRepository, never()).findAll();
        verify(jpaPagoRepository, never()).findByAgricultorId(any());
    }

    @Test
    @DisplayName("suma solo las nóminas pendientes y aprobadas")
    void pendientePorPagar() {
        when(nominaRepository.resumenPorEstado(7L, null)).thenReturn(List.of(
                new ResumenNominas(EstadoNomina.PENDIENTE, 2, new BigDecimal("100")),
                new ResumenNominas(EstadoNomina.APROBADA, 1, new BigDecimal("40")),
                new ResumenNominas(EstadoNomina.PAGADA, 5, new BigDecimal("999")),
                new ResumenNominas(EstadoNomina.ANULADA, 1, new BigDecimal("999"))));

        PendingPayroll p = adapter.getPendingPayroll(7L);

        assertThat(p.nominas()).isEqualTo(3);
        assertThat(p.total()).isEqualByComparingTo("140");
        verify(nominaRepository, never()).listarPorAgricultor(any());
        verify(nominaRepository, never()).listarTodas();
    }

    @Test
    @DisplayName("sin nóminas pendientes devuelve cero")
    void sinPendientes() {
        when(nominaRepository.resumenPorEstado(null, null)).thenReturn(List.of());

        PendingPayroll p = adapter.getPendingPayroll(null);

        assertThat(p.nominas()).isZero();
        assertThat(p.total()).isEqualByComparingTo("0");
    }
}
