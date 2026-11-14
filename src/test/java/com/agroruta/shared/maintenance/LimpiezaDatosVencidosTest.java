package com.agroruta.shared.maintenance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LimpiezaDatosVencidos")
class LimpiezaDatosVencidosTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    /** 20 de noviembre de 2026, 5:17 a. m. en Colombia */
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-20T10:17:00Z"), BOGOTA);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final LimpiezaDatosVencidos limpieza = new LimpiezaDatosVencidos(jdbc, reloj);

    @Test
    @DisplayName("sesiones: vencidas desde ayer, reemplazadas o cerradas hace más de una semana")
    void sesiones() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 5, 17);
        when(jdbc.update(contains("FROM sesiones"), any(), any(), any())).thenReturn(141);

        assertThat(limpieza.limpiar()).containsEntry("sesiones", 141);

        verify(jdbc).update("DELETE FROM sesiones WHERE expira_en < ? OR rotado_en < ? OR revocado_en < ?",
                ahora.minusDays(1), ahora.minusDays(7), ahora.minusDays(7));
    }

    @Test
    @DisplayName("enlaces de correo: los que vencieron hace más de una semana")
    void enlaces() {
        LocalDateTime haceUnaSemana = LocalDateTime.of(2026, 11, 13, 5, 17);
        when(jdbc.update(contains("verificaciones_correo"), any(Object.class))).thenReturn(9);
        when(jdbc.update(contains("restablecimientos_contrasena"), any(Object.class))).thenReturn(4);

        assertThat(limpieza.limpiar())
                .containsEntry("verificacionesCorreo", 9)
                .containsEntry("restablecimientosContrasena", 4);

        verify(jdbc).update("DELETE FROM verificaciones_correo WHERE expira_en < ?", haceUnaSemana);
        verify(jdbc).update("DELETE FROM restablecimientos_contrasena WHERE expira_en < ?", haceUnaSemana);
    }

    @Test
    @DisplayName("contador de diagnósticos: los días de hace más de un mes")
    void usoDiagnosticos() {
        when(jdbc.update(contains("diagnosticos_uso"), any(Object.class))).thenReturn(2);

        assertThat(limpieza.limpiar()).containsEntry("usoDiagnosticos", 2);

        verify(jdbc).update("DELETE FROM diagnosticos_uso WHERE dia < ?", Date.valueOf(LocalDate.of(2026, 10, 21)));
    }

    @Test
    @DisplayName("responde las cuatro tablas aunque no haya nada que borrar")
    void sinNada() {
        assertThat(limpieza.limpiar()).containsOnlyKeys(
                "sesiones", "verificacionesCorreo", "restablecimientosContrasena", "usoDiagnosticos");
    }
}
