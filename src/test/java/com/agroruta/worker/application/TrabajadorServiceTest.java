package com.agroruta.worker.application;

import com.agroruta.worker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias - TrabajadorService")
class TrabajadorServiceTest {

    @Mock private TrabajadorRepository trabajadorRepository;
    @Mock private CargoRepository      cargoRepository;

    @InjectMocks
    private TrabajadorService trabajadorService;

    private Cargo      cargoBase;
    private Cargo      cargoNuevo;
    private Trabajador trabajadorActivo;
    private Trabajador trabajadorInactivo;
    private Trabajador trabajadorSuspendido;

    @BeforeEach
    void setUp() {
        cargoBase  = new Cargo(1L, "Operario",   "Cargo base",  new BigDecimal("80000"),  true);
        cargoNuevo = new Cargo(2L, "Supervisor", "Cargo nuevo", new BigDecimal("120000"), true);

        trabajadorActivo = new Trabajador(1L, "Carlos", "Pérez", "123456",
                "3001234567", "Calle 1", LocalDate.of(2022, 1, 10),
                TipoContrato.JORNAL, cargoBase);

        trabajadorInactivo = new Trabajador(2L, "Ana", "Gómez", "654321",
                "3009876543", "Calle 2", LocalDate.of(2021, 3, 5),
                TipoContrato.TEMPORAL, cargoBase);
        trabajadorInactivo.desactivar();

        trabajadorSuspendido = new Trabajador(3L, "Luis", "Torres", "111222",
                "3007654321", "Calle 3", LocalDate.of(2020, 7, 1),
                TipoContrato.FIJO, cargoBase);
        trabajadorSuspendido.suspender();
    }

    // ── registrarTrabajador ───────────────────────────────────────────────

    @Test
    @DisplayName("registrarTrabajador debe crear y guardar el trabajador con estado ACTIVO")
    void registrarTrabajador_datosValidos_debeCrearYGuardar() {
        when(trabajadorRepository.existePorCedula("999999")).thenReturn(false);
        when(cargoRepository.buscarPorId(1L)).thenReturn(Optional.of(cargoBase));
        when(trabajadorRepository.guardar(any(Trabajador.class))).thenReturn(trabajadorActivo);

        Trabajador resultado = trabajadorService.registrarTrabajador(
                "Carlos", "Pérez", "999999", "3001234567", "Calle 1",
                LocalDate.of(2022, 1, 10), TipoContrato.JORNAL, 1L);

        assertNotNull(resultado);
        assertTrue(resultado.estaActivo());
        verify(trabajadorRepository).guardar(any(Trabajador.class));
    }

    @Test
    @DisplayName("registrarTrabajador debe asignar el cargo correcto al trabajador")
    void registrarTrabajador_debeAsignarCargo() {
        when(trabajadorRepository.existePorCedula("999999")).thenReturn(false);
        when(cargoRepository.buscarPorId(1L)).thenReturn(Optional.of(cargoBase));
        when(trabajadorRepository.guardar(any(Trabajador.class))).thenAnswer(inv -> inv.getArgument(0));

        Trabajador resultado = trabajadorService.registrarTrabajador(
                "Carlos", "Pérez", "999999", "3001234567", "Calle 1",
                LocalDate.of(2022, 1, 10), TipoContrato.JORNAL, 1L);

        assertEquals(cargoBase, resultado.getCargo());
    }

    @Test
    @DisplayName("registrarTrabajador debe lanzar IllegalArgumentException cuando la cédula ya existe")
    void registrarTrabajador_cedulaDuplicada_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.existePorCedula("123456")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.registrarTrabajador(
                        "Carlos", "Pérez", "123456", "3001234567", "Calle 1",
                        LocalDate.of(2022, 1, 10), TipoContrato.JORNAL, 1L)
        );

        assertTrue(ex.getMessage().contains("123456"));
        verify(trabajadorRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("registrarTrabajador debe lanzar IllegalArgumentException cuando el cargo no existe")
    void registrarTrabajador_cargoInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.existePorCedula("999999")).thenReturn(false);
        when(cargoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.registrarTrabajador(
                        "Carlos", "Pérez", "999999", "3001234567", "Calle 1",
                        LocalDate.of(2022, 1, 10), TipoContrato.JORNAL, 99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(trabajadorRepository, never()).guardar(any());
    }

    // ── actualizarTrabajador ──────────────────────────────────────────────

    @Test
    @DisplayName("actualizarTrabajador debe modificar los campos editables y guardar")
    void actualizarTrabajador_idExiste_debeActualizarYGuardar() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(trabajadorRepository.guardar(any(Trabajador.class))).thenReturn(trabajadorActivo);

        Trabajador resultado = trabajadorService.actualizarTrabajador(
                1L, "Carlos Updated", "Pérez Updated",
                "3001111111", "Nueva Calle", TipoContrato.FIJO);

        assertNotNull(resultado);
        verify(trabajadorRepository).guardar(trabajadorActivo);
        assertEquals("Carlos Updated",  trabajadorActivo.getNombre());
        assertEquals("Pérez Updated",   trabajadorActivo.getApellido());
        assertEquals("3001111111",      trabajadorActivo.getTelefono());
        assertEquals("Nueva Calle",     trabajadorActivo.getDireccion());
        assertEquals(TipoContrato.FIJO, trabajadorActivo.getTipoContrato());
    }

    @Test
    @DisplayName("actualizarTrabajador debe lanzar IllegalArgumentException cuando el id no existe")
    void actualizarTrabajador_idInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.actualizarTrabajador(
                        99L, "Nombre", "Apellido", "Tel", "Dir", TipoContrato.JORNAL)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(trabajadorRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarTrabajador no debe alterar cédula, cargo ni estado")
    void actualizarTrabajador_noDebeAlterarCamposInmutables() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(trabajadorRepository.guardar(any())).thenReturn(trabajadorActivo);

        trabajadorService.actualizarTrabajador(
                1L, "Nuevo", "Nombre", "Tel", "Dir", TipoContrato.TEMPORAL);

        assertEquals("123456",        trabajadorActivo.getCedula());
        assertEquals(cargoBase,       trabajadorActivo.getCargo());
        assertTrue(trabajadorActivo.estaActivo());
    }

    // ── cambiarCargo ──────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarCargo debe asignar el nuevo cargo y guardar")
    void cambiarCargo_datosValidos_debeAsignarCargoYGuardar() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(cargoRepository.buscarPorId(2L)).thenReturn(Optional.of(cargoNuevo));
        when(trabajadorRepository.guardar(any())).thenReturn(trabajadorActivo);

        Trabajador resultado = trabajadorService.cambiarCargo(1L, 2L);

        assertNotNull(resultado);
        assertEquals(cargoNuevo, trabajadorActivo.getCargo());
        verify(trabajadorRepository).guardar(trabajadorActivo);
    }

    @Test
    @DisplayName("cambiarCargo debe lanzar IllegalArgumentException cuando el trabajador no existe")
    void cambiarCargo_trabajadorInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.cambiarCargo(99L, 2L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(trabajadorRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("cambiarCargo debe lanzar IllegalArgumentException cuando el cargo no existe")
    void cambiarCargo_cargoInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(cargoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.cambiarCargo(1L, 99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(trabajadorRepository, never()).guardar(any());
    }

    // ── desactivarTrabajador ──────────────────────────────────────────────

    @Test
    @DisplayName("desactivarTrabajador debe cambiar estado a INACTIVO y guardar")
    void desactivarTrabajador_idExiste_debeDesactivarYGuardar() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(trabajadorRepository.guardar(any())).thenReturn(trabajadorActivo);

        trabajadorService.desactivarTrabajador(1L);

        assertFalse(trabajadorActivo.estaActivo());
        assertEquals(EstadoTrabajador.INACTIVO, trabajadorActivo.getEstado());
        verify(trabajadorRepository).guardar(trabajadorActivo);
    }

    @Test
    @DisplayName("desactivarTrabajador debe lanzar IllegalArgumentException cuando el id no existe")
    void desactivarTrabajador_idInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.desactivarTrabajador(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(trabajadorRepository, never()).guardar(any());
    }

    // ── suspenderTrabajador ───────────────────────────────────────────────

    @Test
    @DisplayName("suspenderTrabajador debe cambiar estado a SUSPENDIDO y guardar")
    void suspenderTrabajador_idExiste_debeSuspenderYGuardar() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(trabajadorRepository.guardar(any())).thenReturn(trabajadorActivo);

        trabajadorService.suspenderTrabajador(1L);

        assertEquals(EstadoTrabajador.SUSPENDIDO, trabajadorActivo.getEstado());
        verify(trabajadorRepository).guardar(trabajadorActivo);
    }

    @Test
    @DisplayName("suspenderTrabajador debe lanzar IllegalArgumentException cuando el id no existe")
    void suspenderTrabajador_idInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.suspenderTrabajador(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(trabajadorRepository, never()).guardar(any());
    }

    // ── reactivarTrabajador ───────────────────────────────────────────────

    @Test
    @DisplayName("reactivarTrabajador debe cambiar estado a ACTIVO y guardar")
    void reactivarTrabajador_idExiste_debeReactivarYGuardar() {
        when(trabajadorRepository.buscarPorId(2L)).thenReturn(Optional.of(trabajadorInactivo));
        when(trabajadorRepository.guardar(any())).thenReturn(trabajadorInactivo);

        trabajadorService.reactivarTrabajador(2L);

        assertTrue(trabajadorInactivo.estaActivo());
        assertEquals(EstadoTrabajador.ACTIVO, trabajadorInactivo.getEstado());
        verify(trabajadorRepository).guardar(trabajadorInactivo);
    }

    @Test
    @DisplayName("reactivarTrabajador debe funcionar también desde estado SUSPENDIDO")
    void reactivarTrabajador_desdeSuspendido_debeReactivar() {
        when(trabajadorRepository.buscarPorId(3L)).thenReturn(Optional.of(trabajadorSuspendido));
        when(trabajadorRepository.guardar(any())).thenReturn(trabajadorSuspendido);

        trabajadorService.reactivarTrabajador(3L);

        assertEquals(EstadoTrabajador.ACTIVO, trabajadorSuspendido.getEstado());
    }

    @Test
    @DisplayName("reactivarTrabajador debe lanzar IllegalArgumentException cuando el id no existe")
    void reactivarTrabajador_idInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.reactivarTrabajador(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(trabajadorRepository, never()).guardar(any());
    }

    // ── buscarPorId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId debe retornar el trabajador cuando el id existe")
    void buscarPorId_idExiste_debeRetornarTrabajador() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));

        Trabajador resultado = trabajadorService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Carlos Pérez", resultado.getNombreCompleto());
    }

    @Test
    @DisplayName("buscarPorId debe lanzar IllegalArgumentException cuando el id no existe")
    void buscarPorId_idInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trabajadorService.buscarPorId(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── listarActivos ─────────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivos debe retornar solo los trabajadores activos")
    void listarActivos_debeRetornarSoloActivos() {
        when(trabajadorRepository.listarActivos()).thenReturn(List.of(trabajadorActivo));

        List<Trabajador> resultado = trabajadorService.listarActivos();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).estaActivo());
        verify(trabajadorRepository).listarActivos();
    }

    @Test
    @DisplayName("listarActivos debe retornar lista vacía cuando no hay activos")
    void listarActivos_sinActivos_debeRetornarListaVacia() {
        when(trabajadorRepository.listarActivos()).thenReturn(List.of());

        assertTrue(trabajadorService.listarActivos().isEmpty());
    }

    // ── listarTodos ───────────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos debe retornar trabajadores de todos los estados")
    void listarTodos_debeRetornarTodos() {
        when(trabajadorRepository.listarTodos())
                .thenReturn(List.of(trabajadorActivo, trabajadorInactivo, trabajadorSuspendido));

        List<Trabajador> resultado = trabajadorService.listarTodos();

        assertEquals(3, resultado.size());
        verify(trabajadorRepository).listarTodos();
    }

    @Test
    @DisplayName("listarTodos debe retornar lista vacía cuando no hay trabajadores")
    void listarTodos_sinTrabajadores_debeRetornarListaVacia() {
        when(trabajadorRepository.listarTodos()).thenReturn(List.of());

        assertTrue(trabajadorService.listarTodos().isEmpty());
    }
}