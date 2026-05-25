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
@DisplayName("Pruebas unitarias - JornalService")
class JornalServiceTest {

    @Mock private JornalRepository     jornalRepository;
    @Mock private TrabajadorRepository trabajadorRepository;
    @Mock private ActividadRepository  actividadRepository;

    @InjectMocks
    private JornalService jornalService;

    private Cargo        cargoBase;
    private Trabajador   trabajadorActivo;
    private Trabajador   trabajadorInactivo;
    private Actividad    actividad1;
    private Actividad    actividad2;
    private Jornal       jornalBase;
    private LocalDate    fecha;

    @BeforeEach
    void setUp() {
        cargoBase = new Cargo(1L, "Operario", "Cargo base", new BigDecimal("80000"), true);

        trabajadorActivo = new Trabajador(1L, "Carlos", "Pérez", "123456",
                "3001234567", "Calle 1", LocalDate.of(2022, 1, 10),
                TipoContrato.JORNAL, cargoBase);

        trabajadorInactivo = new Trabajador(2L, "Ana", "Gómez", "654321",
                "3009876543", "Calle 2", LocalDate.of(2021, 3, 5),
                TipoContrato.JORNAL, cargoBase);
        trabajadorInactivo.desactivar();

        actividad1 = new Actividad(1L, "Siembra",   "Desc siembra");
        actividad2 = new Actividad(2L, "Fumigación", "Desc fumigación");

        fecha = LocalDate.of(2024, 6, 15);

        jornalBase = new Jornal(1L, fecha, trabajadorActivo, 10L, "Cultivo A", "Sin observaciones");
    }

    // ── registrarJornal ───────────────────────────────────────────────────

    @Test
    @DisplayName("registrarJornal debe crear jornal con actividades y retornarlo guardado")
    void registrarJornal_datosValidos_debeCrearYGuardar() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividad1));
        when(actividadRepository.buscarPorId(2L)).thenReturn(Optional.of(actividad2));
        when(jornalRepository.guardar(any(Jornal.class))).thenReturn(jornalBase);

        Jornal resultado = jornalService.registrarJornal(
                1L, 10L, "Cultivo A", fecha, List.of(1L, 2L), "Sin observaciones");

        assertNotNull(resultado);
        verify(jornalRepository).guardar(any(Jornal.class));
    }

    @Test
    @DisplayName("registrarJornal debe asignar valorJornal desde el cargo del trabajador")
    void registrarJornal_debeAsignarValorJornalDesdeCargo() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(jornalRepository.guardar(any(Jornal.class))).thenAnswer(inv -> inv.getArgument(0));

        Jornal resultado = jornalService.registrarJornal(
                1L, 10L, "Cultivo A", fecha, List.of(), "Obs");

        assertEquals(new BigDecimal("80000"), resultado.getValorJornal());
    }

    @Test
    @DisplayName("registrarJornal debe lanzar IllegalArgumentException cuando el trabajador no existe")
    void registrarJornal_trabajadorInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jornalService.registrarJornal(
                        99L, 10L, "Cultivo A", fecha, List.of(), "Obs")
        );

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(jornalRepository);
    }

    @Test
    @DisplayName("registrarJornal debe lanzar IllegalStateException cuando el trabajador no está activo")
    void registrarJornal_trabajadorInactivo_debeLanzarIllegalStateException() {
        when(trabajadorRepository.buscarPorId(2L)).thenReturn(Optional.of(trabajadorInactivo));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> jornalService.registrarJornal(
                        2L, 10L, "Cultivo A", fecha, List.of(), "Obs")
        );

        assertTrue(ex.getMessage().contains("Ana Gómez"));
        verifyNoInteractions(jornalRepository);
    }

    @Test
    @DisplayName("registrarJornal debe lanzar IllegalArgumentException cuando una actividad no existe")
    void registrarJornal_actividadInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(actividadRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jornalService.registrarJornal(
                        1L, 10L, "Cultivo A", fecha, List.of(99L), "Obs")
        );

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(jornalRepository);
    }

    @Test
    @DisplayName("registrarJornal sin actividades debe guardar jornal con lista vacía")
    void registrarJornal_sinActividades_debeGuardarJornalVacio() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajadorActivo));
        when(jornalRepository.guardar(any(Jornal.class))).thenAnswer(inv -> inv.getArgument(0));

        Jornal resultado = jornalService.registrarJornal(
                1L, 10L, "Cultivo A", fecha, List.of(), "Obs");

        assertTrue(resultado.getActividades().isEmpty());
        verify(jornalRepository).guardar(any(Jornal.class));
    }

    // ── agregarActividad ──────────────────────────────────────────────────

    @Test
    @DisplayName("agregarActividad debe agregar la actividad al jornal y guardarlo")
    void agregarActividad_datosValidos_debeAgregarYGuardar() {
        when(jornalRepository.buscarPorId(1L)).thenReturn(Optional.of(jornalBase));
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividad1));
        when(jornalRepository.guardar(any(Jornal.class))).thenReturn(jornalBase);

        Jornal resultado = jornalService.agregarActividad(1L, 1L);

        assertNotNull(resultado);
        verify(jornalRepository).guardar(jornalBase);
    }

    @Test
    @DisplayName("agregarActividad debe lanzar IllegalArgumentException cuando el jornal no existe")
    void agregarActividad_jornalInexistente_debeLanzarIllegalArgumentException() {
        when(jornalRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jornalService.agregarActividad(99L, 1L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(actividadRepository);
    }

    @Test
    @DisplayName("agregarActividad debe lanzar IllegalArgumentException cuando la actividad no existe")
    void agregarActividad_actividadInexistente_debeLanzarIllegalArgumentException() {
        when(jornalRepository.buscarPorId(1L)).thenReturn(Optional.of(jornalBase));
        when(actividadRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jornalService.agregarActividad(1L, 99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(jornalRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("agregarActividad debe lanzar IllegalStateException cuando el jornal está liquidado")
    void agregarActividad_jornalLiquidado_debeLanzarIllegalStateException() {
        jornalBase.marcarComoLiquidado();
        when(jornalRepository.buscarPorId(1L)).thenReturn(Optional.of(jornalBase));
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividad1));

        assertThrows(
                IllegalStateException.class,
                () -> jornalService.agregarActividad(1L, 1L)
        );

        verify(jornalRepository, never()).guardar(any());
    }

    // ── removerActividad ──────────────────────────────────────────────────

    @Test
    @DisplayName("removerActividad debe quitar la actividad del jornal y guardarlo")
    void removerActividad_datosValidos_debeRemoverYGuardar() {
        jornalBase.agregarActividad(actividad1);
        when(jornalRepository.buscarPorId(1L)).thenReturn(Optional.of(jornalBase));
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividad1));
        when(jornalRepository.guardar(any(Jornal.class))).thenReturn(jornalBase);

        Jornal resultado = jornalService.removerActividad(1L, 1L);

        assertNotNull(resultado);
        verify(jornalRepository).guardar(jornalBase);
    }

    @Test
    @DisplayName("removerActividad debe lanzar IllegalArgumentException cuando el jornal no existe")
    void removerActividad_jornalInexistente_debeLanzarIllegalArgumentException() {
        when(jornalRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jornalService.removerActividad(99L, 1L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(actividadRepository);
    }

    @Test
    @DisplayName("removerActividad debe lanzar IllegalArgumentException cuando la actividad no existe")
    void removerActividad_actividadInexistente_debeLanzarIllegalArgumentException() {
        when(jornalRepository.buscarPorId(1L)).thenReturn(Optional.of(jornalBase));
        when(actividadRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jornalService.removerActividad(1L, 99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(jornalRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("removerActividad debe lanzar IllegalStateException cuando el jornal está liquidado")
    void removerActividad_jornalLiquidado_debeLanzarIllegalStateException() {
        jornalBase.agregarActividad(actividad1);
        jornalBase.marcarComoLiquidado();
        when(jornalRepository.buscarPorId(1L)).thenReturn(Optional.of(jornalBase));
        when(actividadRepository.buscarPorId(1L)).thenReturn(Optional.of(actividad1));

        assertThrows(
                IllegalStateException.class,
                () -> jornalService.removerActividad(1L, 1L)
        );

        verify(jornalRepository, never()).guardar(any());
    }

    // ── buscarPorId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId debe retornar el jornal cuando el id existe")
    void buscarPorId_idExiste_debeRetornarJornal() {
        when(jornalRepository.buscarPorId(1L)).thenReturn(Optional.of(jornalBase));

        Jornal resultado = jornalService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    @DisplayName("buscarPorId debe lanzar IllegalArgumentException cuando el id no existe")
    void buscarPorId_idInexistente_debeLanzarIllegalArgumentException() {
        when(jornalRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jornalService.buscarPorId(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── listarPorTrabajador ───────────────────────────────────────────────

    @Test
    @DisplayName("listarPorTrabajador debe retornar los jornales del trabajador")
    void listarPorTrabajador_debeRetornarLista() {
        when(jornalRepository.buscarPorTrabajador(1L)).thenReturn(List.of(jornalBase));

        List<Jornal> resultado = jornalService.listarPorTrabajador(1L);

        assertEquals(1, resultado.size());
        verify(jornalRepository).buscarPorTrabajador(1L);
    }

    @Test
    @DisplayName("listarPorTrabajador debe retornar lista vacía cuando no hay jornales")
    void listarPorTrabajador_sinJornales_debeRetornarListaVacia() {
        when(jornalRepository.buscarPorTrabajador(1L)).thenReturn(List.of());

        assertTrue(jornalService.listarPorTrabajador(1L).isEmpty());
    }

    // ── listarPorTrabajadorYPeriodo ───────────────────────────────────────

    @Test
    @DisplayName("listarPorTrabajadorYPeriodo debe retornar jornales en el rango de fechas")
    void listarPorTrabajadorYPeriodo_debeRetornarLista() {
        LocalDate inicio = LocalDate.of(2024, 6, 1);
        LocalDate fin    = LocalDate.of(2024, 6, 30);
        when(jornalRepository.buscarPorTrabajadorYPeriodo(1L, inicio, fin))
                .thenReturn(List.of(jornalBase));

        List<Jornal> resultado = jornalService.listarPorTrabajadorYPeriodo(1L, inicio, fin);

        assertEquals(1, resultado.size());
        verify(jornalRepository).buscarPorTrabajadorYPeriodo(1L, inicio, fin);
    }

    @Test
    @DisplayName("listarPorTrabajadorYPeriodo debe retornar lista vacía cuando no hay jornales en el periodo")
    void listarPorTrabajadorYPeriodo_sinJornales_debeRetornarListaVacia() {
        LocalDate inicio = LocalDate.of(2024, 1, 1);
        LocalDate fin    = LocalDate.of(2024, 1, 31);
        when(jornalRepository.buscarPorTrabajadorYPeriodo(1L, inicio, fin)).thenReturn(List.of());

        assertTrue(jornalService.listarPorTrabajadorYPeriodo(1L, inicio, fin).isEmpty());
    }

    // ── listarPorCultivo ──────────────────────────────────────────────────

    @Test
    @DisplayName("listarPorCultivo debe retornar los jornales del cultivo")
    void listarPorCultivo_debeRetornarLista() {
        when(jornalRepository.buscarPorCultivo(10L)).thenReturn(List.of(jornalBase));

        List<Jornal> resultado = jornalService.listarPorCultivo(10L);

        assertEquals(1, resultado.size());
        verify(jornalRepository).buscarPorCultivo(10L);
    }

    @Test
    @DisplayName("listarPorCultivo debe retornar lista vacía cuando no hay jornales")
    void listarPorCultivo_sinJornales_debeRetornarListaVacia() {
        when(jornalRepository.buscarPorCultivo(10L)).thenReturn(List.of());

        assertTrue(jornalService.listarPorCultivo(10L).isEmpty());
    }

    // ── listarPorCultivoYPeriodo ──────────────────────────────────────────

    @Test
    @DisplayName("listarPorCultivoYPeriodo debe retornar jornales en el rango de fechas")
    void listarPorCultivoYPeriodo_debeRetornarLista() {
        LocalDate inicio = LocalDate.of(2024, 6, 1);
        LocalDate fin    = LocalDate.of(2024, 6, 30);
        when(jornalRepository.buscarPorCultivoYPeriodo(10L, inicio, fin))
                .thenReturn(List.of(jornalBase));

        List<Jornal> resultado = jornalService.listarPorCultivoYPeriodo(10L, inicio, fin);

        assertEquals(1, resultado.size());
        verify(jornalRepository).buscarPorCultivoYPeriodo(10L, inicio, fin);
    }

    @Test
    @DisplayName("listarPorCultivoYPeriodo debe retornar lista vacía cuando no hay jornales en el periodo")
    void listarPorCultivoYPeriodo_sinJornales_debeRetornarListaVacia() {
        LocalDate inicio = LocalDate.of(2024, 1, 1);
        LocalDate fin    = LocalDate.of(2024, 1, 31);
        when(jornalRepository.buscarPorCultivoYPeriodo(10L, inicio, fin)).thenReturn(List.of());

        assertTrue(jornalService.listarPorCultivoYPeriodo(10L, inicio, fin).isEmpty());
    }
}