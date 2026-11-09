package com.agroruta.crop.application;

import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.application.ports.out.LoteRepository;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FincaServiceTest {

    @Mock
    private FincaRepository fincaRepository;

    @Mock
    private LoteRepository loteRepository;

    @InjectMocks
    private FincaService service;

    private final Long agricultorId = 1L;

    @Test
    void deberiaRegistrarFincaCorrectamente() {
        Finca finca = new Finca(1L, "Finca El Paraíso", "Cundinamarca", 50.0, agricultorId);
        when(fincaRepository.existsByNombreAndAgricultorId("Finca El Paraíso", agricultorId)).thenReturn(false);
        when(fincaRepository.save(any())).thenReturn(finca);

        Finca resultado = service.registrarFinca("Finca El Paraíso", "Cundinamarca", 50.0, agricultorId);

        assertNotNull(resultado);
        assertEquals("Finca El Paraíso", resultado.getNombre());
        verify(fincaRepository).save(any());
    }

    @Test
    void deberiaLanzarExcepcionSiFincaYaExiste() {
        when(fincaRepository.existsByNombreAndAgricultorId("Finca El Paraíso", agricultorId)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                service.registrarFinca("Finca El Paraíso", "Cundinamarca", 50.0, agricultorId));
    }

    @Test
    void deberiaBuscarFincaPorId() {
        Finca finca = new Finca(1L, "Finca", "Ubicacion", 10.0, agricultorId);
        when(fincaRepository.findById(1L)).thenReturn(Optional.of(finca));

        Finca resultado = service.buscarFincaPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    void deberiaLanzarExcepcionSiFincaNoExiste() {
        when(fincaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.buscarFincaPorId(99L));
    }

    @Test
    void deberiaListarFincasPorAgricultor() {
        List<Finca> fincas = List.of(
                new Finca(1L, "Finca A", "Loc A", 10.0, agricultorId),
                new Finca(2L, "Finca B", "Loc B", 20.0, agricultorId)
        );
        when(fincaRepository.findByAgricultorId(agricultorId)).thenReturn(fincas);

        List<Finca> resultado = service.listarFincasPorAgricultor(agricultorId);

        assertEquals(2, resultado.size());
        verify(fincaRepository).findByAgricultorId(agricultorId);
    }

    @Test
    void deberiaEliminarFinca() {
        Finca finca = new Finca(1L, "Finca", "Ubicacion", 10.0, agricultorId);
        Lote lote = new Lote(5L, "Lote A", 2.0, 1L);
        when(fincaRepository.findById(1L)).thenReturn(Optional.of(finca));
        when(loteRepository.findByFincaId(1L)).thenReturn(List.of(lote));

        service.eliminarFinca(1L);

        // Borrado lógico de la finca y de sus lotes: las siembras siguen en los reportes
        assertTrue(finca.estaEliminada());
        assertTrue(lote.estaEliminado());
        assertEquals(finca.getEliminadoEn(), lote.getEliminadoEn());
        verify(fincaRepository).save(finca);
        verify(loteRepository).save(lote);
    }

    @Test
    void noSeBorraUnaFincaConUnaSiembraEnCurso() {
        Finca finca = new Finca(1L, "Finca", "Ubicacion", 10.0, agricultorId);
        Lote lote = new Lote(5L, "Lote A", 2.0, 1L);
        when(fincaRepository.findById(1L)).thenReturn(Optional.of(finca));
        when(loteRepository.findByFincaId(1L)).thenReturn(List.of(lote));
        when(loteRepository.existsSiembraActivaEnLote(5L)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.eliminarFinca(1L));

        assertTrue(e.getMessage().contains("Lote A"));
        assertFalse(finca.estaEliminada());
        verify(fincaRepository, never()).save(any());
        verify(loteRepository, never()).save(any());
    }

    @Test
    void deberiaLanzarExcepcionAlEliminarFincaInexistente() {
        when(fincaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.eliminarFinca(99L));
    }

    @Test
    void deberiaActualizarCentroideFinca() {
        Finca finca = new Finca(1L, "Finca", "Ubicacion", 10.0, agricultorId);
        when(fincaRepository.findById(1L)).thenReturn(Optional.of(finca));
        when(fincaRepository.save(any())).thenReturn(finca);

        Finca resultado = service.actualizarCentroideFinca(1L, 4.71, -74.07);

        assertEquals(4.71, resultado.getCentroideLat());
        assertEquals(-74.07, resultado.getCentroideLng());
        verify(fincaRepository).save(any());
    }
}