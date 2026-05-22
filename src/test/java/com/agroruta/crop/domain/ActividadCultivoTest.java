package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ActividadCultivoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 4, 1);

    @Test
    void constructorCompleto_asignaTodosLosCampos() {
        ActividadCultivo actividad = new ActividadCultivo(
                1L, TipoActividad.RIEGO, "Riego inicial", FECHA, 10L
        );
//verificar que cada campo quede bien asignado
        assertThat(actividad.getId()).isEqualTo(1L);
        assertThat(actividad.getTipo()).isEqualTo(TipoActividad.RIEGO);
        assertThat(actividad.getDescripcion()).isEqualTo("Riego inicial");
        assertThat(actividad.getFecha()).isEqualTo(FECHA);
        assertThat(actividad.getSiembraId()).isEqualTo(10L);
    }

    @Test
    void constructorVacio_dejaTodosLosCamposNulos() {
        ActividadCultivo actividad = new ActividadCultivo();

        assertThat(actividad.getId()).isNull();
        assertThat(actividad.getTipo()).isNull();
        assertThat(actividad.getDescripcion()).isNull();
        assertThat(actividad.getFecha()).isNull();
        assertThat(actividad.getSiembraId()).isNull();
    }

    @Test
    void setters_modificanCadaCampoIndependientemente() {
        ActividadCultivo actividad = new ActividadCultivo();

        actividad.setId(5L);
        actividad.setTipo(TipoActividad.DESHIERBE);
        actividad.setDescripcion("Deshierbe manual");
        actividad.setFecha(FECHA);
        actividad.setSiembraId(20L);

        assertThat(actividad.getId()).isEqualTo(5L);
        assertThat(actividad.getTipo()).isEqualTo(TipoActividad.DESHIERBE);
        assertThat(actividad.getDescripcion()).isEqualTo("Deshierbe manual");
        assertThat(actividad.getFecha()).isEqualTo(FECHA);
        assertThat(actividad.getSiembraId()).isEqualTo(20L);
    }

    @Test
    void setter_sobreescribeValorPrevio() {
        ActividadCultivo actividad = new ActividadCultivo(
                1L, TipoActividad.RIEGO, "Riego", FECHA, 10L
        );

        actividad.setDescripcion("Descripción actualizada");
        actividad.setTipo(TipoActividad.PODA);  // ← este sí existe

        assertThat(actividad.getDescripcion()).isEqualTo("Descripción actualizada");
        assertThat(actividad.getTipo()).isEqualTo(TipoActividad.PODA);
    }
}