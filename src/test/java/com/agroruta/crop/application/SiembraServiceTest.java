package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.LoteUseCase;
import com.agroruta.crop.domain.*;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SiembraServiceTest {

    @Mock
    private SiembraRepository siembraRepository;

    @Mock
    private LoteUseCase loteUseCase;

    @Mock
    private CatalogoCultivoRepository catalogo;

    @InjectMocks
    private SiembraService service;

    private final Long loteId = 1L;
    private final LocalDate fecha = LocalDate.of(2026, 1, 10);

    @BeforeEach
    void setUp() {
        Lote lote = new Lote(loteId, "Lote A", 10.0, 1L);
        when(loteUseCase.buscarLotePorId(loteId)).thenReturn(lote);
        when(catalogo.buscarVariedad(1L)).thenReturn(Optional.of(Variedades.COLOMBIA));
    }

    @Test
    void deberiaGuardarCultivoYVariedadDelCatalogo() {
        when(siembraRepository.existsByLoteId(loteId)).thenReturn(false);
        when(siembraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Siembra resultado = service.registrarSiembra(fecha, 500, 1L, loteId);

        assertEquals("Uchuva", resultado.getTipoCultivo());
        assertEquals("Colombia", resultado.getVariedad());
        assertEquals(1L, resultado.getVariedadId());
        assertEquals("Uchuva · Colombia", resultado.nombreCompleto());
    }

    @Test
    void deberiaRechazarVariedadInexistente() {
        when(siembraRepository.existsByLoteId(loteId)).thenReturn(false);
        when(catalogo.buscarVariedad(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.registrarSiembra(fecha, 500, 99L, loteId));
        verify(siembraRepository, never()).save(any());
    }

    @Test
    void deberiaRechazarVariedadDesactivada() {
        when(siembraRepository.existsByLoteId(loteId)).thenReturn(false);
        when(catalogo.buscarVariedad(5L)).thenReturn(Optional.of(
                new VariedadCultivo(5L, "Caturra", false, 2L, "Café", true)));

        assertThrows(BusinessException.class, () ->
                service.registrarSiembra(fecha, 500, 5L, loteId));
    }

    @Test
    void deberiaRechazarCultivoDesactivado() {
        when(siembraRepository.existsByLoteId(loteId)).thenReturn(false);
        when(catalogo.buscarVariedad(6L)).thenReturn(Optional.of(
                new VariedadCultivo(6L, "Hass", true, 3L, "Aguacate", false)));

        assertThrows(BusinessException.class, () ->
                service.registrarSiembra(fecha, 500, 6L, loteId));
    }

    @Test
    void deberiaRegistrarSiembraCorrectamente() {
        Siembra siembra = new Siembra(1L, fecha, 500, Variedades.COLOMBIA, loteId);
        when(siembraRepository.existsByLoteId(loteId)).thenReturn(false);
        when(siembraRepository.save(any())).thenReturn(siembra);

        Siembra resultado = service.registrarSiembra(fecha, 500, 1L, loteId);

        assertNotNull(resultado);
        assertEquals(Variedades.COLOMBIA.getNombre(), resultado.getVariedad());
        verify(siembraRepository).save(any());
    }

    @Test
    void deberiaLanzarExcepcionSiLoteYaTieneSiembra() {
        when(siembraRepository.existsByLoteId(loteId)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                service.registrarSiembra(fecha, 500, 1L, loteId));
    }

    @Test
    void deberiaBuscarSiembraPorId() {
        Siembra siembra = new Siembra(1L, fecha, 500, Variedades.KENYA, loteId);
        when(siembraRepository.findById(1L)).thenReturn(Optional.of(siembra));

        Siembra resultado = service.buscarSiembraPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    void deberiaLanzarExcepcionSiSiembraNoExiste() {
        when(siembraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.buscarSiembraPorId(99L));
    }

    @Test
    void deberiaBuscarSiembraPorLote() {
        Siembra siembra = new Siembra(1L, fecha, 500, Variedades.GIGANTE, loteId);
        when(siembraRepository.findByLoteId(loteId)).thenReturn(Optional.of(siembra));

        Siembra resultado = service.buscarSiembraPorLote(loteId);

        assertNotNull(resultado);
        assertEquals(loteId, resultado.getLoteId());
    }

    @Test
    void deberiaLanzarExcepcionSiNoHaySiembraEnLote() {
        when(siembraRepository.findByLoteId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.buscarSiembraPorLote(99L));
    }

    @Test
    void deberiaAvanzarEtapa() {
        Siembra siembra = new Siembra(1L, fecha, 500, Variedades.COLOMBIA, loteId);
        when(siembraRepository.findById(1L)).thenReturn(Optional.of(siembra));
        when(siembraRepository.save(any())).thenReturn(siembra);

        Siembra resultado = service.avanzarEtapa(1L);

        assertEquals(EstadoCultivo.CRECIMIENTO, resultado.getEstadoCultivo());
        verify(siembraRepository).save(any());
    }

    @Test
    void deberiaListarSiembrasPorEstado() {
        List<Siembra> siembras = List.of(
                new Siembra(1L, fecha, 100, Variedades.COLOMBIA, loteId),
                new Siembra(2L, fecha, 200, Variedades.GIGANTE, 2L)
        );
        when(siembraRepository.findByEstadoCultivo(EstadoCultivo.GERMINACION)).thenReturn(siembras);

        List<Siembra> resultado = service.listarSiembrasPorEstado("GERMINACION");

        assertEquals(2, resultado.size());
        verify(siembraRepository).findByEstadoCultivo(EstadoCultivo.GERMINACION);
    }

    @Test
    void deberiaEliminarSiembra() {
        Siembra siembra = new Siembra(1L, fecha, 500, Variedades.COLOMBIA, loteId);
        when(siembraRepository.findById(1L)).thenReturn(Optional.of(siembra));

        service.eliminarSiembra(1L);

        verify(siembraRepository).deleteById(1L);
    }

    @Test
    void deberiaLanzarExcepcionAlEliminarSiembraInexistente() {
        when(siembraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.eliminarSiembra(99L));
    }
}