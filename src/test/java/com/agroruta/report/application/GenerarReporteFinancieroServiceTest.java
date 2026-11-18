package com.agroruta.report.application;

import com.agroruta.report.application.ports.out.CostosQueryPort;
import com.agroruta.report.application.ports.out.CropQueryPort;
import com.agroruta.report.domain.CostosTemporada;
import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.ReporteFinanciero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerarReporteFinancieroService")
class GenerarReporteFinancieroServiceTest {

    @Mock private CropQueryPort cropQueryPort;
    @Mock private CostosQueryPort costosQueryPort;
    @InjectMocks private GenerarReporteFinancieroService service;

    private final CropDetail s1 = new CropDetail(1L, "Café · Castillo", "PRODUCCION", LocalDate.of(2026, 1, 1), 300, 50.0, 2);
    private final CropDetail s2 = new CropDetail(2L, "Uchuva · Colombia", "FINALIZADO", LocalDate.of(2025, 1, 1), 600, 0.0, 0);
    private final CostosTemporada c1 = new CostosTemporada(100, 2, 50, 0, 150, 400, 50, 50, 250.0, 3.0);

    @Test
    @DisplayName("junta cada temporada del agricultor con sus costos; sin costos queda en null")
    void juntaCostos() {
        when(cropQueryPort.getCropDetailsByAgricultor(7L)).thenReturn(List.of(s1, s2));
        when(costosQueryPort.costosDe(1L)).thenReturn(Optional.of(c1));
        when(costosQueryPort.costosDe(2L)).thenReturn(Optional.empty());

        ReporteFinanciero r = service.generateFor(7L);

        assertThat(r.generatedAt()).isNotNull();
        assertThat(r.temporadas()).hasSize(2);
        assertThat(r.temporadas().get(0).costos()).isEqualTo(c1);
        assertThat(r.temporadas().get(1).costos()).isNull();
        verify(cropQueryPort, never()).getAllCropDetails();
    }

    @Test
    @DisplayName("el administrador ve todas las temporadas")
    void admin() {
        when(cropQueryPort.getAllCropDetails()).thenReturn(List.of(s1));
        when(costosQueryPort.costosDe(1L)).thenReturn(Optional.of(c1));

        assertThat(service.generate().temporadas()).hasSize(1);
    }

    @Test
    @DisplayName("sin agricultor el reporte sale vacío y no consulta nada")
    void sinAgricultor() {
        assertThat(service.generateFor(null).temporadas()).isEmpty();
        verifyNoInteractions(cropQueryPort, costosQueryPort);
    }
}
