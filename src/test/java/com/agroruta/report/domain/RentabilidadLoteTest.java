package com.agroruta.report.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("RentabilidadLote")
class RentabilidadLoteTest {

    private static CropDetail siembra(long id, Long loteId, String lote, LocalDate fecha, String estado, Double area) {
        return new CropDetail(id, "Café · Castillo", estado, fecha, 100, 0.0, 0, "Café", lote, "La Esperanza",
                false, loteId, area, null);
    }

    private static CostosTemporada costos(double costo, double ingresos, Double ganancia) {
        return new CostosTemporada(costo, 10, 0, 0, costo, ingresos, 100, ganancia != null ? 100 : 0, ganancia, costo / 100);
    }

    @Test
    @DisplayName("agrupa por lote, compara cada temporada con la anterior y deja la más reciente primero")
    void agrupaYCompara() {
        List<RentabilidadLote> lotes = RentabilidadLote.porLote(List.of(
                new TemporadaFinanzas(siembra(1, 4L, "Lote A", LocalDate.of(2025, 1, 10), "FINALIZADO", 2.0),
                        costos(1_000_000, 1_500_000, 500_000.0)),
                new TemporadaFinanzas(siembra(2, 4L, "Lote A", LocalDate.of(2026, 1, 10), "PRODUCCION", 2.0),
                        costos(1_200_000, 2_000_000, 800_000.0))));

        assertThat(lotes).hasSize(1);
        RentabilidadLote l = lotes.get(0);
        assertThat(l.ganancia()).isEqualTo(1_300_000.0);
        assertThat(l.gananciaPorHa()).isEqualTo(650_000.0);
        assertThat(l.margen()).isCloseTo(1_300_000.0 / 3_500_000 * 100, within(0.001));
        assertThat(l.temporadas()).extracting(RentabilidadLote.Temporada::siembraId).containsExactly(2L, 1L);
        assertThat(l.temporadas().get(0).cambio()).isEqualTo(300_000.0);
        assertThat(l.temporadas().get(0).enCurso()).isTrue();
        assertThat(l.temporadas().get(1).cambio()).isNull();
        assertThat(l.temporadas().get(1).retorno()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("primero los lotes que más dejaron; al final los que no tienen ventas")
    void ordena() {
        List<RentabilidadLote> lotes = RentabilidadLote.porLote(List.of(
                new TemporadaFinanzas(siembra(1, 1L, "Sin ventas", LocalDate.of(2026, 1, 1), "PRODUCCION", null),
                        costos(300_000, 0, null)),
                new TemporadaFinanzas(siembra(2, 2L, "Pierde", LocalDate.of(2026, 1, 1), "FINALIZADO", null),
                        costos(900_000, 400_000, -500_000.0)),
                new TemporadaFinanzas(siembra(3, 3L, "Gana", LocalDate.of(2026, 1, 1), "FINALIZADO", null),
                        costos(100_000, 600_000, 500_000.0)),
                new TemporadaFinanzas(siembra(4, 3L, "Gana", LocalDate.of(2026, 2, 1), "FINALIZADO", null), null)));

        assertThat(lotes).extracting(RentabilidadLote::loteNombre).containsExactly("Gana", "Pierde", "Sin ventas");
        assertThat(lotes.get(0).temporadas()).hasSize(1); // la temporada sin costos no entra
        assertThat(lotes.get(2).ganancia()).isNull();
        assertThat(lotes.get(0).gananciaPorHa()).isNull(); // sin área
    }

    @Test
    @DisplayName("las siembras antiguas sin lote se agrupan por finca y nombre de lote")
    void sinLoteId() {
        List<RentabilidadLote> lotes = RentabilidadLote.porLote(List.of(
                new TemporadaFinanzas(siembra(1, null, "Lote viejo", LocalDate.of(2024, 1, 1), "FINALIZADO", null),
                        costos(100, 200, 100.0)),
                new TemporadaFinanzas(siembra(2, null, "Lote viejo", LocalDate.of(2025, 1, 1), "FINALIZADO", null),
                        costos(100, 50, -50.0))));

        assertThat(lotes).hasSize(1);
        assertThat(lotes.get(0).temporadas().get(0).cambio()).isEqualTo(-150.0);
    }
}
