package com.agroruta.report.application;

import com.agroruta.report.application.ports.out.PayrollQueryPort;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.WorkerPayrollDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link GeneratePayrollReportService}.
 *
 * Qué se verifica:
 *  - generatedAt se asigna en el momento de la llamada.
 *  - totalAccumulated viene exactamente del puerto.
 *  - workerDetails se propaga sin modificaciones.
 *  - Los dos métodos del puerto se invocan exactamente una vez.
 *  - Casos borde: acumulado cero, lista vacía, múltiples trabajadores.
 *  - generateFor: solo consulta los pagos del agricultor; con id nulo devuelve vacío.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeneratePayrollReportService")
class GeneratePayrollReportServiceTest {

    @Mock
    private PayrollQueryPort payrollQueryPort;

    @InjectMocks
    private GeneratePayrollReportService service;

    // ── fixture ──────────────────────────────────────────────────────────────
    private WorkerPayrollDetail detalleMock(Long id, String nombre, BigDecimal total) {
        return new WorkerPayrollDetail(
                id,
                nombre,
                3,
                total,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31),
                22
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // generate()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("debería retornar un PayrollReport no nulo")
        void deberiaRetornarReporteNoNulo() {
            when(payrollQueryPort.getTotalAccumulated()).thenReturn(BigDecimal.ZERO);
            when(payrollQueryPort.getDetailPerWorker()).thenReturn(List.of());

            PayrollReport reporte = service.generate();

            assertThat(reporte).isNotNull();
        }

        @Test
        @DisplayName("debería asignar generatedAt con la fecha y hora actuales")
        void deberiaAsignarGeneratedAt() {
            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
            when(payrollQueryPort.getTotalAccumulated()).thenReturn(BigDecimal.ZERO);
            when(payrollQueryPort.getDetailPerWorker()).thenReturn(List.of());

            PayrollReport reporte = service.generate();

            LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
            assertThat(reporte.generatedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(despues);
        }

        @Test
        @DisplayName("debería propagar totalAccumulated exactamente desde el puerto")
        void deberiaPropagar_totalAccumulated() {
            BigDecimal esperado = new BigDecimal("4850000.75");
            when(payrollQueryPort.getTotalAccumulated()).thenReturn(esperado);
            when(payrollQueryPort.getDetailPerWorker()).thenReturn(List.of());

            PayrollReport reporte = service.generate();

            assertThat(reporte.totalAccumulated())
                    .isEqualByComparingTo(esperado);
        }

        @Test
        @DisplayName("debería retornar totalAccumulated en cero cuando no hay pagos")
        void deberiaRetornarCero_cuandoNoPagos() {
            when(payrollQueryPort.getTotalAccumulated()).thenReturn(BigDecimal.ZERO);
            when(payrollQueryPort.getDetailPerWorker()).thenReturn(List.of());

            PayrollReport reporte = service.generate();

            assertThat(reporte.totalAccumulated()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("debería propagar la lista de detalles por trabajador sin modificarla")
        void deberiaPropagar_workerDetails() {
            List<WorkerPayrollDetail> detalles = List.of(
                    detalleMock(1L, "Carlos López",  new BigDecimal("1500000")),
                    detalleMock(2L, "María Herrera", new BigDecimal("1200000"))
            );
            when(payrollQueryPort.getTotalAccumulated()).thenReturn(new BigDecimal("2700000"));
            when(payrollQueryPort.getDetailPerWorker()).thenReturn(detalles);

            PayrollReport reporte = service.generate();

            assertThat(reporte.workerDetails())
                    .hasSize(2)
                    .containsExactlyElementsOf(detalles);
        }

        @Test
        @DisplayName("debería retornar lista vacía cuando no hay trabajadores")
        void deberiaRetornarListaVacia_cuandoNoTrabajadores() {
            when(payrollQueryPort.getTotalAccumulated()).thenReturn(BigDecimal.ZERO);
            when(payrollQueryPort.getDetailPerWorker()).thenReturn(List.of());

            PayrollReport reporte = service.generate();

            assertThat(reporte.workerDetails()).isEmpty();
        }

        @Test
        @DisplayName("debería invocar getTotalAccumulated y getDetailPerWorker exactamente una vez")
        void deberiaConsultarAmbosMetodosDelPuertoUnaVez() {
            when(payrollQueryPort.getTotalAccumulated()).thenReturn(BigDecimal.ZERO);
            when(payrollQueryPort.getDetailPerWorker()).thenReturn(List.of());

            service.generate();

            verify(payrollQueryPort, times(1)).getTotalAccumulated();
            verify(payrollQueryPort, times(1)).getDetailPerWorker();
            verify(payrollQueryPort).getMonthlyPayments(isNull(), any());
            verify(payrollQueryPort).getPendingPayroll(isNull());
            verifyNoMoreInteractions(payrollQueryPort);
        }

        @Test
        @DisplayName("debería preservar todos los campos de cada WorkerPayrollDetail")
        void deberiaPreservarCamposDelDetalle() {
            WorkerPayrollDetail detalle = new WorkerPayrollDetail(
                    7L,
                    "Luis Martínez",
                    5,
                    new BigDecimal("2100000.50"),
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 2, 28),
                    18
            );
            when(payrollQueryPort.getTotalAccumulated()).thenReturn(detalle.totalPaid());
            when(payrollQueryPort.getDetailPerWorker()).thenReturn(List.of(detalle));

            PayrollReport reporte = service.generate();
            WorkerPayrollDetail resultado = reporte.workerDetails().get(0);

            assertThat(resultado.workerId()).isEqualTo(7L);
            assertThat(resultado.workerName()).isEqualTo("Luis Martínez");
            assertThat(resultado.totalPagos()).isEqualTo(5);
            assertThat(resultado.totalPaid()).isEqualByComparingTo("2100000.50");
            assertThat(resultado.periodoInicio()).isEqualTo(LocalDate.of(2025, 2, 1));
            assertThat(resultado.periodoFin()).isEqualTo(LocalDate.of(2025, 2, 28));
            assertThat(resultado.totalJornales()).isEqualTo(18);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // generateFor(agricultorId)
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("generateFor(agricultorId)")
    class GenerateFor {

        @Test
        @DisplayName("debería consultar solo los pagos del agricultor")
        void deberiaConsultarSoloLosDelAgricultor() {
            List<WorkerPayrollDetail> detalles =
                    List.of(detalleMock(1L, "Carlos López", new BigDecimal("1500000")));
            when(payrollQueryPort.getTotalAccumulatedByAgricultor(7L))
                    .thenReturn(new BigDecimal("1500000"));
            when(payrollQueryPort.getDetailPerWorkerByAgricultor(7L)).thenReturn(detalles);

            PayrollReport reporte = service.generateFor(7L);

            assertThat(reporte.totalAccumulated()).isEqualByComparingTo("1500000");
            assertThat(reporte.workerDetails()).containsExactlyElementsOf(detalles);
            verify(payrollQueryPort).getTotalAccumulatedByAgricultor(7L);
            verify(payrollQueryPort).getDetailPerWorkerByAgricultor(7L);
            verify(payrollQueryPort).getMonthlyPayments(eq(7L), any());
            verify(payrollQueryPort).getPendingPayroll(7L);
            verifyNoMoreInteractions(payrollQueryPort);
        }

        @Test
        @DisplayName("con id nulo debería devolver un reporte en cero sin consultar")
        void conIdNuloDeberiaDevolverVacio() {
            PayrollReport reporte = service.generateFor(null);

            assertThat(reporte.totalAccumulated()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(reporte.workerDetails()).isEmpty();
            verifyNoInteractions(payrollQueryPort);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("datos nuevos del reporte")
    class DatosNuevos {

        @Test
        @DisplayName("ordena a los trabajadores de mayor a menor pagado")
        void ordenaPorTotalPagado() {
            when(payrollQueryPort.getDetailPerWorkerByAgricultor(7L)).thenReturn(List.of(
                    detalleMock(1L, "Poco", new BigDecimal("100")),
                    detalleMock(2L, "Mucho", new BigDecimal("900"))));

            PayrollReport reporte = service.generateFor(7L);

            assertThat(reporte.workerDetails()).extracting(WorkerPayrollDetail::workerName)
                    .containsExactly("Mucho", "Poco");
        }

        @Test
        @DisplayName("incluye pendiente por pagar y la serie de 6 meses desde el día 1")
        void incluyePendienteYSerie() {
            when(payrollQueryPort.getPendingPayroll(7L))
                    .thenReturn(new com.agroruta.report.application.ports.out.PayrollQueryPort.PendingPayroll(2, new BigDecimal("300")));
            java.util.List<com.agroruta.report.domain.MonthlyPayment> serie =
                    List.of(new com.agroruta.report.domain.MonthlyPayment("2026-11", new BigDecimal("50"), 1));
            when(payrollQueryPort.getMonthlyPayments(eq(7L), any())).thenReturn(serie);

            PayrollReport reporte = service.generateFor(7L);

            assertThat(reporte.nominasPendientes()).isEqualTo(2);
            assertThat(reporte.totalPendiente()).isEqualByComparingTo("300");
            assertThat(reporte.pagosPorMes()).isEqualTo(serie);
            org.mockito.ArgumentCaptor<java.time.LocalDate> desde = org.mockito.ArgumentCaptor.forClass(java.time.LocalDate.class);
            verify(payrollQueryPort).getMonthlyPayments(eq(7L), desde.capture());
            assertThat(desde.getValue().getDayOfMonth()).isEqualTo(1);
            assertThat(desde.getValue()).isEqualTo(java.time.LocalDate.now().withDayOfMonth(1).minusMonths(5));
        }

        @Test
        @DisplayName("sin datos del puerto no falla: pendiente en cero y serie vacía")
        void toleraNulos() {
            PayrollReport reporte = service.generateFor(7L);

            assertThat(reporte.totalPendiente()).isEqualByComparingTo("0");
            assertThat(reporte.pagosPorMes()).isEmpty();
            assertThat(reporte.workerDetails()).isEmpty();
        }
    }
}
