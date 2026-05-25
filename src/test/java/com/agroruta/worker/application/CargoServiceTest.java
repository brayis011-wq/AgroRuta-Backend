package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.domain.CargoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias - CargoService")
class CargoServiceTest {

    @Mock
    private CargoRepository cargoRepository;

    @InjectMocks
    private CargoService cargoService;

    private Cargo cargoBase;

    @BeforeEach
    void setUp() {
        cargoBase = new Cargo(1L, "Operario", "Cargo base", new BigDecimal("80000"), true);
    }

    // ── crearCargo ────────────────────────────────────────────────────────

    @Test
    @DisplayName("crearCargo debe guardar y retornar el cargo cuando el nombre no existe")
    void crearCargo_nombreNuevo_debeGuardarYRetornar() {
        when(cargoRepository.existsByNombre("Operario")).thenReturn(false);
        when(cargoRepository.guardar(any(Cargo.class))).thenReturn(cargoBase);

        Cargo resultado = cargoService.crearCargo("Operario", "Cargo base", new BigDecimal("80000"));

        assertNotNull(resultado);
        assertEquals("Operario", resultado.getNombre());
        verify(cargoRepository).guardar(any(Cargo.class));
    }

    @Test
    @DisplayName("crearCargo debe crear el cargo con activo en true")
    void crearCargo_debeCrearConActivoEnTrue() {
        Cargo nuevo = new Cargo(null, "Supervisor", "Desc", new BigDecimal("120000"), true);
        when(cargoRepository.existsByNombre("Supervisor")).thenReturn(false);
        when(cargoRepository.guardar(any(Cargo.class))).thenReturn(nuevo);

        Cargo resultado = cargoService.crearCargo("Supervisor", "Desc", new BigDecimal("120000"));

        assertTrue(resultado.isActivo());
    }

    @Test
    @DisplayName("crearCargo debe lanzar BusinessException cuando el nombre ya existe")
    void crearCargo_nombreDuplicado_debeLanzarBusinessException() {
        when(cargoRepository.existsByNombre("Operario")).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> cargoService.crearCargo("Operario", "Desc", new BigDecimal("80000"))
        );

        assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Operario"));
        verifyNoMoreInteractions(cargoRepository);
    }

    // ── actualizarCargo ───────────────────────────────────────────────────

    @Test
    @DisplayName("actualizarCargo debe modificar y guardar cuando el id existe y el nombre es único")
    void actualizarCargo_idExisteNombreUnico_debeActualizarYGuardar() {
        when(cargoRepository.buscarPorId(1L)).thenReturn(Optional.of(cargoBase));
        when(cargoRepository.existsByNombreAndIdNot("Supervisor", 1L)).thenReturn(false);
        when(cargoRepository.guardar(any(Cargo.class))).thenReturn(cargoBase);

        Cargo resultado = cargoService.actualizarCargo(1L, "Supervisor", "Nueva desc", new BigDecimal("120000"));

        assertNotNull(resultado);
        verify(cargoRepository).guardar(any(Cargo.class));
    }

    @Test
    @DisplayName("actualizarCargo debe lanzar ResourceNotFoundException cuando el id no existe")
    void actualizarCargo_idInexistente_debeLanzarResourceNotFoundException() {
        when(cargoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> cargoService.actualizarCargo(99L, "Supervisor", "Desc", new BigDecimal("120000"))
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("99"));
        verify(cargoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarCargo debe lanzar BusinessException cuando el nombre pertenece a otro cargo")
    void actualizarCargo_nombreDuplicadoEnOtro_debeLanzarBusinessException() {
        when(cargoRepository.buscarPorId(1L)).thenReturn(Optional.of(cargoBase));
        when(cargoRepository.existsByNombreAndIdNot("Supervisor", 1L)).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> cargoService.actualizarCargo(1L, "Supervisor", "Desc", new BigDecimal("120000"))
        );

        assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Supervisor"));
        verify(cargoRepository, never()).guardar(any());
    }

    // ── desactivarCargo ───────────────────────────────────────────────────

    @Test
    @DisplayName("desactivarCargo debe poner activo en false y guardar")
    void desactivarCargo_idExiste_debeDesactivarYGuardar() {
        when(cargoRepository.buscarPorId(1L)).thenReturn(Optional.of(cargoBase));
        when(cargoRepository.guardar(any(Cargo.class))).thenReturn(cargoBase);

        cargoService.desactivarCargo(1L);

        assertFalse(cargoBase.isActivo());
        verify(cargoRepository).guardar(cargoBase);
    }

    @Test
    @DisplayName("desactivarCargo debe lanzar ResourceNotFoundException cuando el id no existe")
    void desactivarCargo_idInexistente_debeLanzarResourceNotFoundException() {
        when(cargoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> cargoService.desactivarCargo(99L)
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("99"));
        verify(cargoRepository, never()).guardar(any());
    }

    // ── buscarPorId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId debe retornar el cargo cuando el id existe")
    void buscarPorId_idExiste_debeRetornarCargo() {
        when(cargoRepository.buscarPorId(1L)).thenReturn(Optional.of(cargoBase));

        Cargo resultado = cargoService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Operario", resultado.getNombre());
    }

    @Test
    @DisplayName("buscarPorId debe lanzar ResourceNotFoundException cuando el id no existe")
    void buscarPorId_idInexistente_debeLanzarResourceNotFoundException() {
        when(cargoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> cargoService.buscarPorId(99L)
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("99"));
    }

    // ── listarActivos ─────────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivos debe retornar solo los cargos activos")
    void listarActivos_debeRetornarListaDeActivos() {
        List<Cargo> activos = List.of(
                new Cargo(1L, "Operario",   "Desc 1", new BigDecimal("80000"),  true),
                new Cargo(2L, "Supervisor", "Desc 2", new BigDecimal("120000"), true)
        );
        when(cargoRepository.listarActivos()).thenReturn(activos);

        List<Cargo> resultado = cargoService.listarActivos();

        assertEquals(2, resultado.size());
        verify(cargoRepository).listarActivos();
    }

    @Test
    @DisplayName("listarActivos debe retornar lista vacía cuando no hay activos")
    void listarActivos_sinActivos_debeRetornarListaVacia() {
        when(cargoRepository.listarActivos()).thenReturn(List.of());

        List<Cargo> resultado = cargoService.listarActivos();

        assertTrue(resultado.isEmpty());
    }

    // ── listarTodos ───────────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos debe retornar todos los cargos sin importar estado")
    void listarTodos_debeRetornarTodosLosCargos() {
        Cargo inactivo = new Cargo(3L, "Auxiliar", "Desc 3", new BigDecimal("60000"), false);

        List<Cargo> todos = List.of(cargoBase, inactivo);
        when(cargoRepository.listarTodos()).thenReturn(todos);

        List<Cargo> resultado = cargoService.listarTodos();

        assertEquals(2, resultado.size());
        verify(cargoRepository).listarTodos();
    }

    @Test
    @DisplayName("listarTodos debe retornar lista vacía cuando no hay cargos")
    void listarTodos_sinCargos_debeRetornarListaVacia() {
        when(cargoRepository.listarTodos()).thenReturn(List.of());

        List<Cargo> resultado = cargoService.listarTodos();

        assertTrue(resultado.isEmpty());
    }
}