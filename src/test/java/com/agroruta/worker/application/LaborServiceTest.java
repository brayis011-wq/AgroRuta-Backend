package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.domain.Labor;
import com.agroruta.worker.domain.LaborRepository;
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
@DisplayName("Pruebas unitarias - LaborService")
class LaborServiceTest {

    @Mock
    private LaborRepository laborRepository;

    @InjectMocks
    private LaborService laborService;

    private Labor actividadBase;

    @BeforeEach
    void setUp() {
        actividadBase = new Labor(1L, "Siembra", "Labor de siembra");
    }

    // ── crearActividad ────────────────────────────────────────────────────

    @Test
    @DisplayName("crearActividad debe guardar y retornar la actividad cuando el nombre no existe")
    void crearActividad_nombreNuevo_debeGuardarYRetornar() {
        when(laborRepository.existeNombreVisible("Siembra", null, null)).thenReturn(false);
        when(laborRepository.guardar(any(Labor.class))).thenReturn(actividadBase);

        Labor resultado = laborService.crearActividad("Siembra", "Labor de siembra");

        assertNotNull(resultado);
        assertEquals("Siembra", resultado.getNombre());
        verify(laborRepository).guardar(any(Labor.class));
    }

    @Test
    @DisplayName("crearActividad debe lanzar BusinessException cuando el nombre ya existe")
    void crearActividad_nombreDuplicado_debeLanzarBusinessException() {
        when(laborRepository.existeNombreVisible("Siembra", null, null)).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> laborService.crearActividad("Siembra", "Descripción")
        );

        assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Siembra"));
        verifyNoMoreInteractions(laborRepository);
    }

    @Test
    @DisplayName("crearActividad debe crear la actividad con activa en true")
    void crearActividad_debeCrearConActivaEnTrue() {
        Labor nueva = new Labor(null, "Riego", "Labor de riego");
        when(laborRepository.existeNombreVisible("Riego", null, null)).thenReturn(false);
        when(laborRepository.guardar(any(Labor.class))).thenReturn(nueva);

        Labor resultado = laborService.crearActividad("Riego", "Labor de riego");

        assertTrue(resultado.isActiva());
    }

    // ── actualizarActividad ───────────────────────────────────────────────

    @Test
    @DisplayName("actualizarActividad debe modificar y guardar cuando el id existe y el nombre es único")
    void actualizarActividad_idExisteNombreUnico_debeActualizarYGuardar() {
        when(laborRepository.buscarPorId(1L)).thenReturn(Optional.of(actividadBase));
        when(laborRepository.existeNombreVisible("Cosecha", null, 1L)).thenReturn(false);
        when(laborRepository.guardar(any(Labor.class))).thenReturn(actividadBase);

        Labor resultado = laborService.actualizarActividad(1L, "Cosecha", "Nueva descripción");

        assertNotNull(resultado);
        verify(laborRepository).guardar(any(Labor.class));
    }

    @Test
    @DisplayName("actualizarActividad debe lanzar ResourceNotFoundException cuando el id no existe")
    void actualizarActividad_idInexistente_debeLanzarResourceNotFoundException() {
        when(laborRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> laborService.actualizarActividad(99L, "Cosecha", "Descripción")
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("99"));
        verify(laborRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarActividad debe lanzar BusinessException cuando el nuevo nombre pertenece a otra actividad")
    void actualizarActividad_nombreDuplicadoEnOtra_debeLanzarBusinessException() {
        when(laborRepository.buscarPorId(1L)).thenReturn(Optional.of(actividadBase));
        when(laborRepository.existeNombreVisible("Cosecha", null, 1L)).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> laborService.actualizarActividad(1L, "Cosecha", "Descripción")
        );

        assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Cosecha"));
        verify(laborRepository, never()).guardar(any());
    }

    // ── desactivarActividad ───────────────────────────────────────────────

    @Test
    @DisplayName("desactivarActividad debe poner activa en false y guardar")
    void desactivarActividad_idExiste_debeDactivarYGuardar() {
        when(laborRepository.buscarPorId(1L)).thenReturn(Optional.of(actividadBase));
        when(laborRepository.guardar(any(Labor.class))).thenReturn(actividadBase);

        laborService.desactivarActividad(1L);

        assertFalse(actividadBase.isActiva());
        verify(laborRepository).guardar(actividadBase);
    }

    @Test
    @DisplayName("desactivarActividad debe lanzar ResourceNotFoundException cuando el id no existe")
    void desactivarActividad_idInexistente_debeLanzarResourceNotFoundException() {
        when(laborRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> laborService.desactivarActividad(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(laborRepository, never()).guardar(any());
    }

    // ── buscarPorId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId debe retornar la actividad cuando el id existe")
    void buscarPorId_idExiste_debeRetornarActividad() {
        when(laborRepository.buscarPorId(1L)).thenReturn(Optional.of(actividadBase));

        Labor resultado = laborService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Siembra", resultado.getNombre());
    }

    @Test
    @DisplayName("buscarPorId debe lanzar ResourceNotFoundException cuando el id no existe")
    void buscarPorId_idInexistente_debeLanzarResourceNotFoundException() {
        when(laborRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> laborService.buscarPorId(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── listarActivas ─────────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivas debe retornar solo las actividades activas")
    void listarActivas_debeRetornarListaDeActivas() {
        List<Labor> activas = List.of(
                new Labor(1L, "Siembra", "Desc 1"),
                new Labor(2L, "Riego",   "Desc 2")
        );
        when(laborRepository.listarActivas()).thenReturn(activas);

        List<Labor> resultado = laborService.listarActivas();

        assertEquals(2, resultado.size());
        verify(laborRepository).listarActivas();
    }

    @Test
    @DisplayName("listarActivas debe retornar lista vacía cuando no hay activas")
    void listarActivas_sinActivas_debeRetornarListaVacia() {
        when(laborRepository.listarActivas()).thenReturn(List.of());

        List<Labor> resultado = laborService.listarActivas();

        assertTrue(resultado.isEmpty());
    }

    // ── listarTodas ───────────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodas debe retornar todas las actividades sin importar estado")
    void listarTodas_debeRetornarTodasLasActividades() {
        Labor inactiva = new Labor(3L, "Fumigación", "Desc 3");
        inactiva.desactivar();

        List<Labor> todas = List.of(actividadBase, inactiva);
        when(laborRepository.listarTodas()).thenReturn(todas);

        List<Labor> resultado = laborService.listarTodas();

        assertEquals(2, resultado.size());
        verify(laborRepository).listarTodas();
    }

    @Test
    @DisplayName("listarTodas debe retornar lista vacía cuando no hay actividades")
    void listarTodas_sinActividades_debeRetornarListaVacia() {
        when(laborRepository.listarTodas()).thenReturn(List.of());

        List<Labor> resultado = laborService.listarTodas();

        assertTrue(resultado.isEmpty());
    }

    // ── labores por agricultor ────────────────────────────────────────────

    @Test
    @DisplayName("crearActividad con agricultor deja la labor a su nombre y revisa solo las que él ve")
    void crearActividad_deUnAgricultor_quedaASuNombre() {
        when(laborRepository.existeNombreVisible("Poda de formación", 7L, null)).thenReturn(false);
        when(laborRepository.guardar(any(Labor.class))).thenAnswer(inv -> inv.getArgument(0));

        Labor resultado = laborService.crearActividad("  Poda   de formación ", "", 7L);

        assertEquals("Poda de formación", resultado.getNombre());
        assertEquals(7L, resultado.getAgricultorId());
        assertFalse(resultado.esComun());
    }

    @Test
    @DisplayName("crearActividad rechaza un nombre vacío")
    void crearActividad_nombreVacio_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> laborService.crearActividad("   ", "", 7L));
        verify(laborRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarActividad revisa el nombre entre las labores del mismo dueño")
    void actualizarActividad_revisaElNombreDelDueno() {
        Labor propia = new Labor(5L, "Deshoje", "");
        propia.setAgricultorId(7L);
        when(laborRepository.buscarPorId(5L)).thenReturn(Optional.of(propia));
        when(laborRepository.existeNombreVisible("Tutorado", 7L, 5L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> laborService.actualizarActividad(5L, "Tutorado", ""));
        verify(laborRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("listarActivasPara delega en el repositorio con el agricultor")
    void listarActivasPara_delega() {
        when(laborRepository.listarActivasPara(7L)).thenReturn(List.of(actividadBase));

        assertEquals(1, laborService.listarActivasPara(7L).size());
    }

    @Test
    @DisplayName("visiblePara: una labor común la ven todos y una propia solo su dueño")
    void visiblePara() {
        Labor comun = new Labor(1L, "Recolección", "");
        Labor propia = new Labor(2L, "Deshoje", "");
        propia.setAgricultorId(7L);

        assertTrue(comun.visiblePara(9L));
        assertTrue(propia.visiblePara(7L));
        assertFalse(propia.visiblePara(9L));
    }
}
