package com.agroruta.crop.domain;

import com.agroruta.crop.domain.AlertaCultivo.Tipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AlertasCultivo")
class AlertasCultivoTest {

    private final LocalDate hoy = LocalDate.of(2026, 11, 5);

    private Siembra siembra() {
        return new Siembra(1L, LocalDate.of(2026, 8, 1), 100, Variedades.COLOMBIA, 9L);
    }

    private Fumigacion fumigacion(LocalDate fecha, Integer carencia, Integer reentrada) {
        Fumigacion f = Fumigacion.fromManualEntry(fecha, "Mancozeb", 1.0, UnidadMedida.KG, 1.0, null, 1L);
        f.fijarRestricciones(new RestriccionesFumigacion(carencia, reentrada));
        return f;
    }

    private List<AlertaCultivo> alertas(Siembra s, List<ActividadCultivo> act, List<Fumigacion> fum) {
        return AlertasCultivo.delDia(new CalendarioCultivo.DatosSiembra(s, 9L, "Lote 1", List.of(), act, fum, List.of()),
                "La Esperanza", hoy);
    }

    private List<Tipo> tipos(List<AlertaCultivo> a) {
        return a.stream().map(AlertaCultivo::tipo).toList();
    }

    @Test
    @DisplayName("una sola fumigación hace 6 días: mañana toca (intervalo de 7)")
    void fumigacionManana() {
        List<AlertaCultivo> a = alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(6), null, null)));
        assertThat(tipos(a)).containsExactly(Tipo.FUMIGACION_MANANA);
        assertThat(a.get(0).finca()).isEqualTo("La Esperanza");
        assertThat(a.get(0).lote()).isEqualTo("Lote 1");
    }

    @Test
    @DisplayName("hace 7 días: hoy toca fumigar")
    void fumigacionHoy() {
        assertThat(tipos(alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(7), null, null)))))
                .containsExactly(Tipo.FUMIGACION_HOY);
    }

    @Test
    @DisplayName("la atrasada se recuerda a 1, 3, 7 y 14 días, no todos los días")
    void atrasadaSoloAlgunosDias() {
        assertThat(tipos(alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(8), null, null)))))
                .containsExactly(Tipo.FUMIGACION_ATRASADA);
        assertThat(alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(9), null, null)))).isEmpty();
        List<AlertaCultivo> tres = alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(10), null, null)));
        assertThat(tipos(tres)).containsExactly(Tipo.FUMIGACION_ATRASADA);
        assertThat(tres.get(0).detalle()).isEqualTo("3");
        assertThat(tres.get(0).fecha()).isEqualTo(hoy.minusDays(3));
    }

    @Test
    @DisplayName("usa el intervalo habitual entre fumigaciones")
    void intervaloHabitual() {
        List<Fumigacion> f = List.of(fumigacion(hoy.minusDays(20), null, null), fumigacion(hoy.minusDays(10), null, null));
        assertThat(tipos(alertas(siembra(), List.of(), f))).containsExactly(Tipo.FUMIGACION_HOY);
    }

    @Test
    @DisplayName("si se fumigó hoy no avisa que toca fumigar")
    void fumigadoHoy() {
        assertThat(tipos(alertas(siembra(), List.of(), List.of(fumigacion(hoy, null, null))))).isEmpty();
    }

    @Test
    @DisplayName("labores programadas para hoy y mañana")
    void labores() {
        List<ActividadCultivo> act = List.of(
                new ActividadCultivo(1L, TipoActividad.RIEGO, "Goteo", hoy, 1L),
                new ActividadCultivo(2L, TipoActividad.PODA, null, hoy.plusDays(1), 1L),
                new ActividadCultivo(3L, TipoActividad.PODA, null, hoy.plusDays(5), 1L));
        List<AlertaCultivo> a = alertas(siembra(), act, List.of());
        assertThat(tipos(a)).containsExactly(Tipo.LABOR_HOY, Tipo.LABOR_MANANA);
        assertThat(a.get(0).titulo()).isEqualTo("RIEGO");
        assertThat(a.get(0).detalle()).isEqualTo("Goteo");
    }

    @Test
    @DisplayName("reentrada: avisa mientras dura y el día que termina; carencia: el día que termina")
    void reentradaYCarencia() {
        assertThat(tipos(alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(1), null, 72)))))
                .contains(Tipo.REENTRADA_EN_CURSO);
        assertThat(tipos(alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(2), null, 48)))))
                .contains(Tipo.REENTRADA_LIBRE);
        // Aplicada hace 3 días con reentrada de 5: ya se avisó al aplicar, no se repite
        assertThat(tipos(alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(3), null, 120)))))
                .doesNotContain(Tipo.REENTRADA_EN_CURSO, Tipo.REENTRADA_LIBRE);
        assertThat(tipos(alertas(siembra(), List.of(), List.of(fumigacion(hoy.minusDays(14), 14, null)))))
                .contains(Tipo.CARENCIA_LIBRE);
    }

    @Test
    @DisplayName("una siembra finalizada no genera alertas")
    void finalizada() {
        Siembra s = siembra();
        s.finalizar(hoy.minusDays(1));
        assertThat(alertas(s, List.of(new ActividadCultivo(1L, TipoActividad.RIEGO, null, hoy, 1L)),
                List.of(fumigacion(hoy.minusDays(7), null, null)))).isEmpty();
    }
}
