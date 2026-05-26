package com.agroruta.report.application;

import com.agroruta.report.application.ports.out.CropQueryPort;
import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.CropReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link GenerateCropReportService}.
 *
 * Qué se verifica:
 *  - El reporte se construye delegando en CropQueryPort.
 *  - generatedAt se genera en el momento de la llamada (no nulo, no futuro).
 *  - La lista de detalles se propaga tal cual desde el puerto.
 *  - Casos borde: lista vacía, un elemento, múltiples elementos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateCropReportService")
class GenerateCropReportServiceTest {

    @Mock
    private CropQueryPort cropQueryPort;

    @InjectMocks
    private GenerateCropReportService service;

    // ── fixture ──────────────────────────────────────────────────────────────
    private CropDetail detalleMock(Long id, String variedad) {
        return new CropDetail(
                id,
                variedad,
                "EN_PRODUCCION",
                LocalDate.of(2025, 1, 10),
                120L,
                350.5,
                3
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // generate()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("debería retornar un CropReport no nulo")
        void deberiaRetornarReporteNoNulo() {
            when(cropQueryPort.getAllCropDetails()).thenReturn(List.of());

            CropReport reporte = service.generate();

            assertThat(reporte).isNotNull();
        }

        @Test
        @DisplayName("debería asignar generatedAt con la fecha y hora actuales")
        void deberiaAsignarGeneratedAt() {
            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
            when(cropQueryPort.getAllCropDetails()).thenReturn(List.of());

            CropReport reporte = service.generate();

            LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
            assertThat(reporte.generatedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(despues);
        }

        @Test
        @DisplayName("debería propagar la lista de detalles del puerto sin modificarla")
        void deberiaPropagaListaDetalles() {
            List<CropDetail> detalles = List.of(
                    detalleMock(1L, "AURORA"),
                    detalleMock(2L, "COLOMBIA")
            );
            when(cropQueryPort.getAllCropDetails()).thenReturn(detalles);

            CropReport reporte = service.generate();

            assertThat(reporte.cropDetails())
                    .hasSize(2)
                    .containsExactlyElementsOf(detalles);
        }

        @Test
        @DisplayName("debería retornar lista vacía cuando el puerto no devuelve cultivos")
        void deberiaRetornarListaVacia_cuandoNoCultivos() {
            when(cropQueryPort.getAllCropDetails()).thenReturn(List.of());

            CropReport reporte = service.generate();

            assertThat(reporte.cropDetails()).isEmpty();
        }

        @Test
        @DisplayName("debería retornar un reporte con un único cultivo")
        void deberiaRetornarUnCultivo_cuandoHayUno() {
            CropDetail detalle = detalleMock(1L, "AURORA");
            when(cropQueryPort.getAllCropDetails()).thenReturn(List.of(detalle));

            CropReport reporte = service.generate();

            assertThat(reporte.cropDetails()).hasSize(1);
            assertThat(reporte.cropDetails().get(0).siembraId()).isEqualTo(1L);
            assertThat(reporte.cropDetails().get(0).variedad()).isEqualTo("AURORA");
        }

        @Test
        @DisplayName("debería invocar getAllCropDetails exactamente una vez por llamada")
        void deberiaConsultarPortUnaVez() {
            when(cropQueryPort.getAllCropDetails()).thenReturn(List.of());

            service.generate();

            verify(cropQueryPort, times(1)).getAllCropDetails();
            verifyNoMoreInteractions(cropQueryPort);
        }

        @Test
        @DisplayName("debería preservar todos los campos de cada CropDetail")
        void deberiaPreservarCamposDelDetalle() {
            CropDetail detalle = new CropDetail(
                    42L,
                    "COLOMBIA",
                    "COSECHADO",
                    LocalDate.of(2024, 6, 1),
                    300L,
                    1200.75,
                    8
            );
            when(cropQueryPort.getAllCropDetails()).thenReturn(List.of(detalle));

            CropReport reporte = service.generate();
            CropDetail resultado = reporte.cropDetails().get(0);

            assertThat(resultado.siembraId()).isEqualTo(42L);
            assertThat(resultado.variedad()).isEqualTo("COLOMBIA");
            assertThat(resultado.estado()).isEqualTo("COSECHADO");
            assertThat(resultado.fechaSiembra()).isEqualTo(LocalDate.of(2024, 6, 1));
            assertThat(resultado.diasDesdeSiembra()).isEqualTo(300L);
            assertThat(resultado.totalKgCosechado()).isEqualTo(1200.75);
            assertThat(resultado.totalCosechas()).isEqualTo(8);
        }
    }
}