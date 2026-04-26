package com.agroruta.cultivo.application;

import com.agroruta.cultivo.domain.ActividadCultivo;
import com.agroruta.cultivo.domain.ActividadCultivoRepository;
import com.agroruta.cultivo.domain.TipoActividad;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActividadCultivoServiceTest {

    @Mock
    private ActividadCultivoRepository actividadRepository;

    @Mock
    private SiembraUseCase siembraUseCase;

    @InjectMocks
    private ActividadCultivoService service;

    private ActividadCultivo actividadMock;

    @BeforeEach
    void setUp() {
        actividadMock = new ActividadCultivo(
                1L, TipoActividad.RIEGO, "Riego inicial", LocalDate.of(2026, 4, 1), 10L
        );
    }

    @Test
    void registrarActividad_exitoso() {
        when(actividadRepository.save(any())).thenReturn(actividadMock);

        ActividadCultivo result = service.registrarActividad(
                "RIEGO", "Riego inicial", LocalDate.of(2026, 4, 1), 10L
        );

        assertThat(result).isNotNull();
        assertThat(result.getTipo()).isEqualTo(TipoActividad.RIEGO);
        assertThat(result.getDescripcion()).isEqualTo("Riego inicial");
        verify(siembraUseCase).buscarSiembraPorId(10L);
        verify(actividadRepository).save(any());
    }

    @Test
    void registrarActividad_descripcionVacia_lanzaExcepcion() {
        assertThatThrownBy(() ->
                service.registrarActividad("RIEGO", "", LocalDate.now(), 10L)
        ).isInstanceOf(BusinessException.class)
                .hasMessageContaining("descripción");

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void registrarActividad_fechaNula_lanzaExcepcion() {
        assertThatThrownBy(() ->
                service.registrarActividad("RIEGO", "Riego", null, 10L)
        ).isInstanceOf(BusinessException.class)
                .hasMessageContaining("fecha");

        verify(actividadRepository, never()).save(any());
    }

    @Test
    void listarActividadesPorSiembra_retornaLista() {
        when(actividadRepository.findBySiembraId(10L)).thenReturn(List.of(actividadMock));

        List<ActividadCultivo> resultado = service.listarActividadesPorSiembra(10L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSiembraId()).isEqualTo(10L);
        verify(siembraUseCase).buscarSiembraPorId(10L);
    }

    @Test
    void listarActividadesPorSiembra_listaVacia() {
        when(actividadRepository.findBySiembraId(99L)).thenReturn(List.of());

        List<ActividadCultivo> resultado = service.listarActividadesPorSiembra(99L);

        assertThat(resultado).isEmpty();
    }
}