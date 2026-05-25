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
@DisplayName("Pruebas unitarias - NominaService")
class NominaServiceTest {

    @Mock private NominaRepository     nominaRepository;
    @Mock private JornalRepository     jornalRepository;
    @Mock private TrabajadorRepository trabajadorRepository;

    @InjectMocks
    private NominaService nominaService;

    private Cargo      cargoBase;
    private Trabajador trabajador;
    private Jornal     jornal1;
    private Jornal     jornal2;
    private Nomina     nominaPendiente;
    private Nomina     nominaAprobada;
    private Nomina     nominaAnulada;
    private Nomina     nominaPagada;
    private LocalDate  inicio;
    private LocalDate  fin;

    @BeforeEach
    void setUp() {
        cargoBase  = new Cargo(1L, "Operario", "Cargo base", new BigDecimal("80000"), true);
        trabajador = new Trabajador(1L, "Carlos", "Pérez", "123456",
                "3001234567", "Calle 1", LocalDate.of(2022, 1, 10),
                TipoContrato.JORNAL, cargoBase);

        inicio = LocalDate.of(2024, 6, 1);
        fin    = LocalDate.of(2024, 6, 30);

        jornal1 = new Jornal(1L, LocalDate.of(2024, 6, 10), trabajador, 10L, "Cultivo A", "");
        jornal2 = new Jornal(2L, LocalDate.of(2024, 6, 15), trabajador, 10L, "Cultivo A", "");

        nominaPendiente = new Nomina(1L, trabajador, inicio, fin, List.of(jornal1, jornal2));

        nominaAprobada = new Nomina(2L, trabajador, inicio, fin, List.of(jornal1));
        nominaAprobada.aprobar();

        nominaAnulada = new Nomina(3L, trabajador, inicio, fin, List.of(jornal1));
        nominaAnulada.anular();

        nominaPagada = new Nomina(4L, trabajador, inicio, fin, List.of(jornal1));
        nominaPagada.aprobar();
        nominaPagada.marcarComoPagada();
    }

    // ── generarNomina ─────────────────────────────────────────────────────

    @Test
    @DisplayName("generarNomina debe crear y guardar la nómina con los jornales disponibles")
    void generarNomina_datosValidos_debeCrearYGuardar() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajador));
        when(jornalRepository.buscarDisponiblesParaNomina(1L, inicio, fin))
                .thenReturn(List.of(jornal1, jornal2));
        when(nominaRepository.guardar(any(Nomina.class))).thenReturn(nominaPendiente);

        Nomina resultado = nominaService.generarNomina(1L, inicio, fin);

        assertNotNull(resultado);
        assertEquals(EstadoNomina.PENDIENTE, resultado.getEstado());
        verify(nominaRepository).guardar(any(Nomina.class));
    }

    @Test
    @DisplayName("generarNomina debe calcular el valorTotal sumando los jornales")
    void generarNomina_debeCalcularValorTotal() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajador));
        when(jornalRepository.buscarDisponiblesParaNomina(1L, inicio, fin))
                .thenReturn(List.of(jornal1, jornal2));
        when(nominaRepository.guardar(any(Nomina.class))).thenAnswer(inv -> inv.getArgument(0));

        Nomina resultado = nominaService.generarNomina(1L, inicio, fin);

        assertEquals(new BigDecimal("160000"), resultado.getValorTotal());
        assertEquals(2, resultado.getTotalJornales());
    }

    @Test
    @DisplayName("generarNomina debe lanzar IllegalArgumentException cuando el trabajador no existe")
    void generarNomina_trabajadorInexistente_debeLanzarIllegalArgumentException() {
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> nominaService.generarNomina(99L, inicio, fin)
        );

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(nominaRepository);
    }

    @Test
    @DisplayName("generarNomina debe lanzar IllegalStateException cuando no hay jornales disponibles")
    void generarNomina_sinJornalesDisponibles_debeLanzarIllegalStateException() {
        when(trabajadorRepository.buscarPorId(1L)).thenReturn(Optional.of(trabajador));
        when(jornalRepository.buscarDisponiblesParaNomina(1L, inicio, fin))
                .thenReturn(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> nominaService.generarNomina(1L, inicio, fin)
        );

        assertTrue(ex.getMessage().contains("Carlos Pérez"));
        verifyNoInteractions(nominaRepository);
    }

    // ── aprobarNomina ─────────────────────────────────────────────────────

    @Test
    @DisplayName("aprobarNomina debe cambiar estado a PAGADA y persistir jornales liquidados")
    void aprobarNomina_nominaPendiente_debeAprobarPagarYPersistirJornales() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPendiente));
        when(nominaRepository.guardar(any(Nomina.class))).thenReturn(nominaPendiente);

        Nomina resultado = nominaService.aprobarNomina(1L);

        assertNotNull(resultado);
        assertEquals(EstadoNomina.PAGADA, resultado.getEstado());
        verify(jornalRepository, times(nominaPendiente.getJornales().size()))
                .guardar(any(Jornal.class));
        verify(nominaRepository).guardar(nominaPendiente);
    }

    @Test
    @DisplayName("aprobarNomina debe marcar todos los jornales como liquidados")
    void aprobarNomina_debeMarcarJornalesComoLiquidados() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPendiente));
        when(nominaRepository.guardar(any(Nomina.class))).thenReturn(nominaPendiente);

        nominaService.aprobarNomina(1L);

        nominaPendiente.getJornales().forEach(j -> assertTrue(j.isLiquidado()));
    }

    @Test
    @DisplayName("aprobarNomina debe lanzar IllegalArgumentException cuando la nómina no existe")
    void aprobarNomina_nominaInexistente_debeLanzarIllegalArgumentException() {
        when(nominaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> nominaService.aprobarNomina(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(nominaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("aprobarNomina debe lanzar IllegalStateException cuando la nómina no está en PENDIENTE")
    void aprobarNomina_nominaNoEnPendiente_debeLanzarIllegalStateException() {
        when(nominaRepository.buscarPorId(3L)).thenReturn(Optional.of(nominaAnulada));

        assertThrows(
                IllegalStateException.class,
                () -> nominaService.aprobarNomina(3L)
        );

        verify(nominaRepository, never()).guardar(any());
    }

    // ── anularNomina ──────────────────────────────────────────────────────

    @Test
    @DisplayName("anularNomina debe cambiar estado a ANULADA y guardar")
    void anularNomina_nominaPendiente_debeAnularYGuardar() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPendiente));
        when(nominaRepository.guardar(any(Nomina.class))).thenReturn(nominaPendiente);

        nominaService.anularNomina(1L);

        assertEquals(EstadoNomina.ANULADA, nominaPendiente.getEstado());
        verify(nominaRepository).guardar(nominaPendiente);
    }

    @Test
    @DisplayName("anularNomina debe lanzar IllegalArgumentException cuando la nómina no existe")
    void anularNomina_nominaInexistente_debeLanzarIllegalArgumentException() {
        when(nominaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> nominaService.anularNomina(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(nominaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("anularNomina debe lanzar IllegalStateException cuando la nómina está PAGADA")
    void anularNomina_nominaPagada_debeLanzarIllegalStateException() {
        when(nominaRepository.buscarPorId(4L)).thenReturn(Optional.of(nominaPagada));

        assertThrows(
                IllegalStateException.class,
                () -> nominaService.anularNomina(4L)
        );

        verify(nominaRepository, never()).guardar(any());
    }

    // ── reactivarNomina ───────────────────────────────────────────────────

    @Test
    @DisplayName("reactivarNomina debe cambiar estado a PENDIENTE cuando está ANULADA")
    void reactivarNomina_nominaAnulada_debeReactivarYGuardar() {
        when(nominaRepository.buscarPorId(3L)).thenReturn(Optional.of(nominaAnulada));
        when(nominaRepository.guardar(any(Nomina.class))).thenReturn(nominaAnulada);

        Nomina resultado = nominaService.reactivarNomina(3L);

        assertNotNull(resultado);
        assertEquals(EstadoNomina.PENDIENTE, nominaAnulada.getEstado());
        verify(nominaRepository).guardar(nominaAnulada);
    }

    @Test
    @DisplayName("reactivarNomina debe lanzar IllegalArgumentException cuando la nómina no existe")
    void reactivarNomina_nominaInexistente_debeLanzarIllegalArgumentException() {
        when(nominaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> nominaService.reactivarNomina(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(nominaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("reactivarNomina debe lanzar IllegalStateException cuando la nómina no está ANULADA")
    void reactivarNomina_nominaNoAnulada_debeLanzarIllegalStateException() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPendiente));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> nominaService.reactivarNomina(1L)
        );

        assertTrue(ex.getMessage().contains("ANULADA"));
        verify(nominaRepository, never()).guardar(any());
    }

    // ── eliminarNomina ────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminarNomina debe eliminar cuando la nómina está en PENDIENTE")
    void eliminarNomina_nominaPendiente_debeEliminar() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPendiente));

        nominaService.eliminarNomina(1L);

        verify(nominaRepository).eliminar(1L);
    }

    @Test
    @DisplayName("eliminarNomina debe lanzar IllegalArgumentException cuando la nómina no existe")
    void eliminarNomina_nominaInexistente_debeLanzarIllegalArgumentException() {
        when(nominaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> nominaService.eliminarNomina(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(nominaRepository, never()).eliminar(any());
    }

    @Test
    @DisplayName("eliminarNomina debe lanzar IllegalStateException cuando la nómina no está en PENDIENTE")
    void eliminarNomina_nominaNoEnPendiente_debeLanzarIllegalStateException() {
        when(nominaRepository.buscarPorId(2L)).thenReturn(Optional.of(nominaAprobada));

        assertThrows(
                IllegalStateException.class,
                () -> nominaService.eliminarNomina(2L)
        );

        verify(nominaRepository, never()).eliminar(any());
    }

    // ── buscarPorId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId debe retornar la nómina cuando el id existe")
    void buscarPorId_idExiste_debeRetornarNomina() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPendiente));

        Nomina resultado = nominaService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    @DisplayName("buscarPorId debe lanzar IllegalArgumentException cuando el id no existe")
    void buscarPorId_idInexistente_debeLanzarIllegalArgumentException() {
        when(nominaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> nominaService.buscarPorId(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── listarPorTrabajador ───────────────────────────────────────────────

    @Test
    @DisplayName("listarPorTrabajador debe retornar las nóminas del trabajador")
    void listarPorTrabajador_debeRetornarLista() {
        when(nominaRepository.buscarPorTrabajador(1L)).thenReturn(List.of(nominaPendiente));

        List<Nomina> resultado = nominaService.listarPorTrabajador(1L);

        assertEquals(1, resultado.size());
        verify(nominaRepository).buscarPorTrabajador(1L);
    }

    @Test
    @DisplayName("listarPorTrabajador debe retornar lista vacía cuando no hay nóminas")
    void listarPorTrabajador_sinNominas_debeRetornarListaVacia() {
        when(nominaRepository.buscarPorTrabajador(1L)).thenReturn(List.of());

        assertTrue(nominaService.listarPorTrabajador(1L).isEmpty());
    }

    // ── listarPendientesPorTrabajador ─────────────────────────────────────

    @Test
    @DisplayName("listarPendientesPorTrabajador debe filtrar por estado PENDIENTE")
    void listarPendientesPorTrabajador_debeRetornarSoloPendientes() {
        when(nominaRepository.buscarPorTrabajadorYEstado(1L, EstadoNomina.PENDIENTE))
                .thenReturn(List.of(nominaPendiente));

        List<Nomina> resultado = nominaService.listarPendientesPorTrabajador(1L);

        assertEquals(1, resultado.size());
        assertEquals(EstadoNomina.PENDIENTE, resultado.get(0).getEstado());
        verify(nominaRepository).buscarPorTrabajadorYEstado(1L, EstadoNomina.PENDIENTE);
    }

    @Test
    @DisplayName("listarPendientesPorTrabajador debe retornar lista vacía cuando no hay pendientes")
    void listarPendientesPorTrabajador_sinPendientes_debeRetornarListaVacia() {
        when(nominaRepository.buscarPorTrabajadorYEstado(1L, EstadoNomina.PENDIENTE))
                .thenReturn(List.of());

        assertTrue(nominaService.listarPendientesPorTrabajador(1L).isEmpty());
    }

    // ── listarPorPeriodo ──────────────────────────────────────────────────

    @Test
    @DisplayName("listarPorPeriodo debe retornar nóminas en el rango de fechas")
    void listarPorPeriodo_debeRetornarLista() {
        when(nominaRepository.buscarPorPeriodo(inicio, fin)).thenReturn(List.of(nominaPendiente));

        List<Nomina> resultado = nominaService.listarPorPeriodo(inicio, fin);

        assertEquals(1, resultado.size());
        verify(nominaRepository).buscarPorPeriodo(inicio, fin);
    }

    @Test
    @DisplayName("listarPorPeriodo debe retornar lista vacía cuando no hay nóminas en el periodo")
    void listarPorPeriodo_sinNominas_debeRetornarListaVacia() {
        when(nominaRepository.buscarPorPeriodo(inicio, fin)).thenReturn(List.of());

        assertTrue(nominaService.listarPorPeriodo(inicio, fin).isEmpty());
    }

    // ── listarTodas ───────────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodas debe retornar todas las nóminas sin importar estado")
    void listarTodas_debeRetornarTodasLasNominas() {
        when(nominaRepository.listarTodas())
                .thenReturn(List.of(nominaPendiente, nominaAprobada, nominaAnulada, nominaPagada));

        List<Nomina> resultado = nominaService.listarTodas();

        assertEquals(4, resultado.size());
        verify(nominaRepository).listarTodas();
    }

    @Test
    @DisplayName("listarTodas debe retornar lista vacía cuando no hay nóminas")
    void listarTodas_sinNominas_debeRetornarListaVacia() {
        when(nominaRepository.listarTodas()).thenReturn(List.of());

        assertTrue(nominaService.listarTodas().isEmpty());
    }
}