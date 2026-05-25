package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.domain.Actividad;
import com.agroruta.worker.domain.ActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Pruebas unitarias - ActividadService")
class ActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @InjectMocks
    private ActividadService actividadService;

    private Actividad actividadBase;

    @BeforeEach
    void setUp() {
        actividadBase = new Actividad(1L, "Siembra", "Actividad de siembra");
    }

    // ── crearActividad ────────────────────────────────────────────────────

    @Test
    @DisplayName("crearActividad debe guardar y retornar la actividad cuando el nombre no existe")
    void crearActividad_nombreNuevo_debeGuardarYRetornar() {
        when(actividadRepository.existsByNombre("Siembra")).thenReturn(false);
        when(actividadRepository.guardar(any(Actividad.class))).thenReturn(actividadBase);

        Actividad resultado = actividadService.crearActividad("Siembra", "Actividad de siembra");

        assertNotNull(resultado);
        assertEquals("Siembra", resultado.getNombre());
        verify(actividadRepository).guardar(any(Actividad.class));
    }

    @Test
    @DisplayName("crearActividad debe lanzar BusinessException cuando el nombre ya existe")
    void crearActividad_nombreDuplicado_debeLanzarBusinessException() {
        when(actividadRepository.existsByNombre("Siembra")).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> actividadService.crearActividad("Siembra", "Descripción")
        );

        assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Siembra"));
        verifyNoMoreInteractions(actividadRepository);
    }

    @Test
    @DisplayName("crearActividad debe crear la actividad con activa en true")
    void crearActividad_debeCrearConActivaEnTrue() {
        Actividad nueva = new Actividad(null, "Riego", "Actividad de riego");
        when(actividadRepository.existsByNombre("Riego")).thenReturn(false);
        when(actividadRepository.guardar(any(Actividad.class))).thenReturn(nueva);

        Actividad resultado = actividadService.crearActividad("Riego", "Actividad de riego");

        assertTrue(resultado.isActiva());
    }

    // ── actualizarActividad ───────────────────────────────────────────────

    @Test
    @DisplayName("actualizarActividad debe modificar y guardar cuando el id existe y el nombre es único")
    void actualizarActividad_idExisteNombreUnico_debeActualizarYGuardar() {
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividadBase));
        when(actividadRepository.existsByNombreAndIdNot("Cosecha", 1L)).thenReturn(false);
        when(actividadRepository.guardar(any(Actividad.class))).thenReturn(actividadBase);

        Actividad resultado = actividadService.actualizarActividad(1L, "Cosecha", "Nueva descripción");

        assertNotNull(resultado);
        verify(actividadRepository).guardar(any(Actividad.class));
    }

    @Test
    @DisplayName("actualizarActividad debe lanzar ResourceNotFoundException cuando el id no existe")
    void actualizarActividad_idInexistente_debeLanzarResourceNotFoundException() {
        when(actividadRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> actividadService.actualizarActividad(99L, "Cosecha", "Descripción")
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("99"));
        verify(actividadRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarActividad debe lanzar BusinessException cuando el nuevo nombre pertenece a otra actividad")
    void actualizarActividad_nombreDuplicadoEnOtra_debeLanzarBusinessException() {
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividadBase));
        when(actividadRepository.existsByNombreAndIdNot("Cosecha", 1L)).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> actividadService.actualizarActividad(1L, "Cosecha", "Descripción")
        );

        assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Cosecha"));
        verify(actividadRepository, never()).guardar(any());
    }

    // ── desactivarActividad ───────────────────────────────────────────────

    @Test
    @DisplayName("desactivarActividad debe poner activa en false y guardar")
    void desactivarActividad_idExiste_debeDactivarYGuardar() {
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividadBase));
        when(actividadRepository.guardar(any(Actividad.class))).thenReturn(actividadBase);

        actividadService.desactivarActividad(1L);

        assertFalse(actividadBase.isActiva());
        verify(actividadRepository).guardar(actividadBase);
    }

    @Test
    @DisplayName("desactivarActividad debe lanzar IllegalArgumentException cuando el id no existe")
    void desactivarActividad_idInexistente_debeLanzarIllegalArgumentException() {
        when(actividadRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> actividadService.desactivarActividad(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(actividadRepository, never()).guardar(any());
    }

    // ── buscarPorId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId debe retornar la actividad cuando el id existe")
    void buscarPorId_idExiste_debeRetornarActividad() {
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividadBase));

        Actividad resultado = actividadService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Siembra", resultado.getNombre());
    }

    @Test
    @DisplayName("buscarPorId debe lanzar IllegalArgumentException cuando el id no existe")
    void buscarPorId_idInexistente_debeLanzarIllegalArgumentException() {
        when(actividadRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> actividadService.buscarPorId(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── listarActivas ─────────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivas debe retornar solo las actividades activas")
    void listarActivas_debeRetornarListaDeActivas() {
        List<Actividad> activas = List.of(
                new Actividad(1L, "Siembra", "Desc 1"),
                new Actividad(2L, "Riego",   "Desc 2")
        );
        when(actividadRepository.listarActivas()).thenReturn(activas);

        List<Actividad> resultado = actividadService.listarActivas();

        assertEquals(2, resultado.size());
        verify(actividadRepository).listarActivas();
    }

    @Test
    @DisplayName("listarActivas debe retornar lista vacía cuando no hay activas")
    void listarActivas_sinActivas_debeRetornarListaVacia() {
        when(actividadRepository.listarActivas()).thenReturn(List.of());

        List<Actividad> resultado = actividadService.listarActivas();

        assertTrue(resultado.isEmpty());
    }

    // ── listarTodas ───────────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodas debe retornar todas las actividades sin importar estado")
    void listarTodas_debeRetornarTodasLasActividades() {
        Actividad inactiva = new Actividad(3L, "Fumigación", "Desc 3");
        inactiva.desactivar();

        List<Actividad> todas = List.of(actividadBase, inactiva);
        when(actividadRepository.listarTodas()).thenReturn(todas);

        List<Actividad> resultado = actividadService.listarTodas();

        assertEquals(2, resultado.size());
        verify(actividadRepository).listarTodas();
    }

    @Test
    @DisplayName("listarTodas debe retornar lista vacía cuando no hay actividades")
    void listarTodas_sinActividades_debeRetornarListaVacia() {
        when(actividadRepository.listarTodas()).thenReturn(List.of());

        List<Actividad> resultado = actividadService.listarTodas();

        assertTrue(resultado.isEmpty());
    }
}