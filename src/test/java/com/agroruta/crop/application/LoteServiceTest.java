package com.agroruta.crop.application;

import com.agroruta.crop.domain.EstadoLote;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import com.agroruta.crop.application.ports.in.FincaUseCase;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.application.ports.out.LoteRepository;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class LoteServiceTest {

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private FincaUseCase fincaUseCase;

    @InjectMocks
    private LoteService service;

    private final Long fincaId = 1L;

    @BeforeEach
    void setUp() {
        Finca finca = new Finca(fincaId, "Finca Test", "Ubicacion", 50.0, 1L);
        when(fincaUseCase.buscarFincaPorId(fincaId)).thenReturn(finca);
    }

    @Test
    void deberiaRegistrarLoteCorrectamente() {
        Lote lote = new Lote(1L, "Lote A", 10.0, fincaId);
        when(loteRepository.existsByNombreAndFincaId("Lote A", fincaId)).thenReturn(false);
        when(loteRepository.save(any())).thenReturn(lote);

        Lote resultado = service.registrarLote("Lote A", 10.0, fincaId);

        assertNotNull(resultado);
        assertEquals("Lote A", resultado.getNombre());
        verify(loteRepository).save(any());
    }

    @Test
    void deberiaLanzarExcepcionSiLoteYaExiste() {
        when(loteRepository.existsByNombreAndFincaId("Lote A", fincaId)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                service.registrarLote("Lote A", 10.0, fincaId));
    }

    @Test
    void deberiaBuscarLotePorId() {
        Lote lote = new Lote(1L, "Lote A", 10.0, fincaId);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        Lote resultado = service.buscarLotePorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    void deberiaLanzarExcepcionSiLoteNoExiste() {
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.buscarLotePorId(99L));
    }

    @Test
    void deberiaListarLotesPorFinca() {
        List<Lote> lotes = List.of(
                new Lote(1L, "Lote A", 10.0, fincaId),
                new Lote(2L, "Lote B", 20.0, fincaId)
        );
        when(loteRepository.findByFincaId(fincaId)).thenReturn(lotes);

        List<Lote> resultado = service.listarLotesPorFinca(fincaId);

        assertEquals(2, resultado.size());
        verify(loteRepository).findByFincaId(fincaId);
    }

    @Test
    void deberiaEliminarLote() {
        Lote lote = new Lote(1L, "Lote A", 10.0, fincaId);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        service.eliminarLote(1L);

        // Borrado lógico: se marca y se guarda, no se borra la fila
        assertTrue(lote.estaEliminado());
        verify(loteRepository).save(lote);
    }

    @Test
    void deberiaLanzarExcepcionAlEliminarLoteInexistente() {
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.eliminarLote(99L));
    }

    @Test
    void deberiaActualizarGeometriaLote() {
        Lote lote = new Lote(1L, "Lote A", 10.0, fincaId);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(loteRepository.save(any())).thenReturn(lote);

        Lote resultado = service.actualizarGeometriaLote(
                1L, "[[4.71,-74.07]]", 15.0, 4.71, -74.07);

        assertEquals("[[4.71,-74.07]]", resultado.getCoordenadas());
        verify(loteRepository).save(any());
    }

    // ── edición y estado ──────────────────────────────────────────────

    @Test
    void editarCambiaNombreYArea() {
        Lote lote = new Lote(1L, "Lote A", 10.0, fincaId);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(loteRepository.existsByNombreAndFincaId("Lote Norte", fincaId)).thenReturn(false);
        when(loteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lote resultado = service.actualizarLote(1L, "Lote Norte", 12.5);

        assertEquals("Lote Norte", resultado.getNombre());
        assertEquals(12.5, resultado.getArea());
    }

    @Test
    void editarRechazaUnNombreQueYaUsaOtroLote() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(new Lote(1L, "Lote A", 10.0, fincaId)));
        when(loteRepository.existsByNombreAndFincaId("Lote B", fincaId)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.actualizarLote(1L, "Lote B", 10.0));
    }

    @Test
    void editarConElMismoNombreNoChocaConsigoMismo() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(new Lote(1L, "Lote A", 10.0, fincaId)));
        when(loteRepository.existsByNombreAndFincaId(any(), any())).thenReturn(true);
        when(loteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(8.0, service.actualizarLote(1L, "lote a", 8.0).getArea());
    }

    @Test
    void cambiarEstadoPoneElLoteEnDescanso() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(new Lote(1L, "Lote A", 10.0, fincaId)));
        when(loteRepository.existsSiembraActivaEnLote(1L)).thenReturn(false);
        when(loteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(EstadoLote.EN_DESCANSO, service.cambiarEstado(1L, "en_descanso").getEstado());
    }

    @Test
    void cambiarEstadoNoSePermiteConSiembraEnCursoNiAEnCultivo() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(new Lote(1L, "Lote A", 10.0, fincaId)));
        when(loteRepository.existsSiembraActivaEnLote(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.cambiarEstado(1L, "DISPONIBLE"));
        assertThrows(BusinessException.class, () -> service.cambiarEstado(1L, "EN_CULTIVO"));
    }

    @Test
    void ocuparYLiberarCambianElEstado() {
        Lote lote = new Lote(1L, "Lote A", 10.0, fincaId);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        service.ocuparLote(1L);
        assertEquals(EstadoLote.EN_CULTIVO, lote.getEstado());

        service.liberarLote(1L, EstadoLote.EN_DESCANSO);
        assertEquals(EstadoLote.EN_DESCANSO, lote.getEstado());
    }

    @Test
    void noSeBorraUnLoteConSiembraEnCurso() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(new Lote(1L, "Lote A", 10.0, fincaId)));
        when(loteRepository.existsSiembraActivaEnLote(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.eliminarLote(1L));
        verify(loteRepository, never()).save(any());
    }
}
