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
class CosechaServiceTest {

    @Mock
    private CosechaRepository cosechaRepository;

    @Mock
    private SiembraUseCase siembraUseCase;

    @InjectMocks
    private CosechaService service;

    private final Long siembraId = 1L;
    private final LocalDate fecha = LocalDate.of(2026, 4, 10);

    @BeforeEach
    void setUp() {
        Siembra siembra = new Siembra(siembraId, fecha, 200, VariedadUchuva.COLOMBIA, 1L);
        when(siembraUseCase.buscarSiembraPorId(siembraId)).thenReturn(siembra);
    }

    @Test
    void deberiaRegistrarCosechaCorrectamente() {
        Cosecha cosecha = new Cosecha(1L, fecha, 300.0, CalidadCosecha.PRIMERA, "Buena", siembraId);
        when(cosechaRepository.existsByFechaAndSiembraId(fecha, siembraId)).thenReturn(false);
        when(cosechaRepository.save(any())).thenReturn(cosecha);

        Cosecha resultado = service.registrarCosecha(fecha, 300.0, "PRIMERA", "Buena", siembraId);

        assertNotNull(resultado);
        assertEquals(CalidadCosecha.PRIMERA, resultado.getCalidad());
        assertEquals(300.0, resultado.getCantidadKg());
        verify(cosechaRepository).save(any());
    }

    @Test
    void deberiaLanzarExcepcionSiCosechaYaExisteEnEsaFecha() {
        when(cosechaRepository.existsByFechaAndSiembraId(fecha, siembraId)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                service.registrarCosecha(fecha, 300.0, "PRIMERA", "Buena", siembraId));
    }

    @Test
    void deberiaListarCosechasPorSiembra() {
        List<Cosecha> cosechas = List.of(
                new Cosecha(1L, fecha, 100.0, CalidadCosecha.PRIMERA, "obs", siembraId),
                new Cosecha(2L, fecha.plusDays(1), 200.0, CalidadCosecha.SEGUNDA, "obs", siembraId)
        );
        when(cosechaRepository.findBySiembraId(siembraId)).thenReturn(cosechas);

        List<Cosecha> resultado = service.listarCosechasPorSiembra(siembraId);

        assertEquals(2, resultado.size());
        verify(cosechaRepository).findBySiembraId(siembraId);
    }

    @Test
    void deberiaRetornarTotalKgCosechado() {
        when(cosechaRepository.totalKgBySiembraId(siembraId)).thenReturn(500.0);

        Double total = service.totalKgCosechado(siembraId);

        assertEquals(500.0, total);
    }

    @Test
    void deberiaRetornarCeroCuandoNoHayCosechas() {
        when(cosechaRepository.totalKgBySiembraId(siembraId)).thenReturn(null);

        Double total = service.totalKgCosechado(siembraId);

        assertEquals(0.0, total);
    }
}