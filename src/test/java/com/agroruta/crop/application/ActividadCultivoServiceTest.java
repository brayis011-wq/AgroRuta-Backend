package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.*;
import com.agroruta.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

    private final Long siembraId = 1L;
    private final LocalDate fecha = LocalDate.of(2026, 3, 10);

    @BeforeEach
    void setUp() {
        Siembra siembra = new Siembra(siembraId, fecha, 100, VariedadUchuva.COLOMBIA, 1L);
        when(siembraUseCase.buscarSiembraPorId(siembraId)).thenReturn(siembra);
    }

    @Test
    void deberiaRegistrarActividadCorrectamente() {
        ActividadCultivo actividad = new ActividadCultivo(
                1L, TipoActividad.RIEGO, "Riego por goteo", fecha, siembraId);
        when(actividadRepository.existsByTipoAndFechaAndSiembraId(
                TipoActividad.RIEGO, fecha, siembraId)).thenReturn(false);
        when(actividadRepository.save(any())).thenReturn(actividad);

        ActividadCultivo resultado = service.registrarActividad(
                "RIEGO", "Riego por goteo", fecha, siembraId);

        assertNotNull(resultado);
        assertEquals(TipoActividad.RIEGO, resultado.getTipo());
        verify(actividadRepository).save(any());
    }

    @Test
    void deberiaLanzarExcepcionSiDescripcionEsNula() {
        assertThrows(BusinessException.class, () ->
                service.registrarActividad("RIEGO", null, fecha, siembraId));
    }

    @Test
    void deberiaLanzarExcepcionSiDescripcionEstaVacia() {
        assertThrows(BusinessException.class, () ->
                service.registrarActividad("RIEGO", "   ", fecha, siembraId));
    }

    @Test
    void deberiaLanzarExcepcionSiFechaEsNula() {
        assertThrows(BusinessException.class, () ->
                service.registrarActividad("RIEGO", "Riego", null, siembraId));
    }

    @Test
    void deberiaLanzarExcepcionSiActividadYaExiste() {
        when(actividadRepository.existsByTipoAndFechaAndSiembraId(
                TipoActividad.PODA, fecha, siembraId)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                service.registrarActividad("PODA", "Poda de formación", fecha, siembraId));
    }

    @Test
    void deberiaListarActividadesPorSiembra() {
        List<ActividadCultivo> actividades = List.of(
                new ActividadCultivo(1L, TipoActividad.RIEGO, "Riego", fecha, siembraId),
                new ActividadCultivo(2L, TipoActividad.PODA, "Poda", fecha, siembraId)
        );
        when(actividadRepository.findBySiembraId(siembraId)).thenReturn(actividades);

        List<ActividadCultivo> resultado = service.listarActividadesPorSiembra(siembraId);

        assertEquals(2, resultado.size());
        verify(actividadRepository).findBySiembraId(siembraId);
    }
}