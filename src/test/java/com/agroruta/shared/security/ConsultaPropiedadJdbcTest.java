package com.agroruta.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultaPropiedadJdbc")
class ConsultaPropiedadJdbcTest {

    @Mock private JdbcTemplate jdbc;

    @InjectMocks private ConsultaPropiedadJdbc consulta;

    @ParameterizedTest
    @EnumSource(Recurso.class)
    @DisplayName("Cada recurso tiene su consulta, con el id y el agricultor como parámetros")
    void cadaRecursoTieneConsulta(Recurso recurso) {
        String sql = ConsultaPropiedadJdbc.consulta(recurso);

        assertThat(sql).startsWith("SELECT COUNT(*) FROM ");
        assertThat(sql).endsWith(".agricultor_id = ?");
        assertThat(sql.chars().filter(c -> c == '?').count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Es dueño cuando la consulta encuentra la fila")
    void esDuenoSiHayFila() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(80L), eq(1L))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(80L), eq(2L))).thenReturn(0);

        assertThat(consulta.esDueno(Recurso.COSECHA, 80L, 1L)).isTrue();
        assertThat(consulta.esDueno(Recurso.COSECHA, 80L, 2L)).isFalse();
        verify(jdbc).queryForObject(ConsultaPropiedadJdbc.consulta(Recurso.COSECHA), Integer.class, 80L, 1L);
    }

    @Test
    @DisplayName("Sin id o sin agricultor no consulta y responde false")
    void sinDatosNoConsulta() {
        assertThat(consulta.esDueno(Recurso.FINCA, null, 1L)).isFalse();
        assertThat(consulta.esDueno(Recurso.FINCA, 10L, null)).isFalse();
        assertThat(consulta.esDueno(null, 10L, 1L)).isFalse();
        verifyNoInteractions(jdbc);
    }
}
