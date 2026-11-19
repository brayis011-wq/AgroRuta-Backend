package com.agroruta.report.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Periodo de los reportes")
class PeriodoTest {

    private static final LocalDate ENE = LocalDate.of(2026, 1, 1);
    private static final LocalDate DIC = LocalDate.of(2026, 12, 31);
    private final Periodo anio2026 = Periodo.de(ENE, DIC);

    private static CropDetail temporada(long id, LocalDate inicio, LocalDate fin) {
        return new CropDetail(id, "Café · Castillo", fin == null ? "PRODUCCION" : "FINALIZADO", inicio, 10, 0.0, 0,
                "Café", "Lote", "Finca", false, 1L, 2.0, fin);
    }

    @Test
    @DisplayName("sin fechas es TODO y deja pasar cualquier temporada")
    void sinFechas() {
        assertThat(Periodo.de(null, null)).isSameAs(Periodo.TODO);
        assertThat(Periodo.TODO.seCruzaCon(LocalDate.of(2001, 1, 1), LocalDate.of(2002, 1, 1))).isTrue();
    }

    @Test
    @DisplayName("una temporada cuenta si estuvo en el campo en algún día del periodo")
    void cruce() {
        assertThat(anio2026.seCruzaCon(LocalDate.of(2025, 9, 22), null)).as("en curso desde 2025").isTrue();
        assertThat(anio2026.seCruzaCon(LocalDate.of(2025, 3, 1), LocalDate.of(2026, 2, 1))).as("terminó en febrero").isTrue();
        assertThat(anio2026.seCruzaCon(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 12, 31))).as("terminó antes").isFalse();
        assertThat(anio2026.seCruzaCon(LocalDate.of(2027, 1, 2), null)).as("sembrada después").isFalse();
        assertThat(anio2026.seCruzaCon(DIC, null)).as("sembrada el último día").isTrue();
    }

    @Test
    @DisplayName("contiene incluye los dos extremos")
    void contiene() {
        assertThat(anio2026.contiene(ENE)).isTrue();
        assertThat(anio2026.contiene(DIC)).isTrue();
        assertThat(anio2026.contiene(DIC.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("la fecha inicial no puede ser posterior a la final")
    void alReves() {
        assertThatThrownBy(() -> Periodo.de(DIC, ENE)).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    @DisplayName("los reportes de cultivos y finanzas se quedan con las temporadas del periodo")
    void filtraReportes() {
        CropDetail vieja = temporada(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 1));
        CropDetail actual = temporada(2, LocalDate.of(2026, 4, 20), null);
        CropReport cultivos = new CropReport(LocalDateTime.now(), List.of(vieja, actual));
        assertThat(cultivos.en(anio2026).cropDetails()).containsExactly(actual);
        assertThat(cultivos.en(Periodo.TODO)).isSameAs(cultivos);

        ReporteFinanciero finanzas = new ReporteFinanciero(LocalDateTime.now(),
                List.of(new TemporadaFinanzas(vieja, null), new TemporadaFinanzas(actual, null)));
        assertThat(finanzas.en(anio2026).temporadas()).extracting(TemporadaFinanzas::siembra).containsExactly(actual);
    }
}
