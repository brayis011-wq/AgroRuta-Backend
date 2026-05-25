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
@DisplayName("Pruebas unitarias - PagoService")
class PagoServiceTest {

    @Mock private PagoRepository   pagoRepository;
    @Mock private NominaRepository nominaRepository;

    @InjectMocks
    private PagoService pagoService;

    private Cargo      cargoBase;
    private Trabajador trabajador;
    private Jornal     jornal;
    private Nomina     nominaPagada;
    private Nomina     nominaPendiente;
    private Pago       pagoBase;

    @BeforeEach
    void setUp() {
        cargoBase  = new Cargo(1L, "Operario", "Cargo base", new BigDecimal("80000"), true);
        trabajador = new Trabajador(1L, "Carlos", "Pérez", "123456",
                "3001234567", "Calle 1", LocalDate.of(2022, 1, 10),
                TipoContrato.JORNAL, cargoBase);

        jornal = new Jornal(1L, LocalDate.of(2024, 6, 10),
                trabajador, 10L, "Cultivo A", "");

        nominaPagada = new Nomina(1L, trabajador,
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30),
                List.of(jornal));
        nominaPagada.aprobar();
        nominaPagada.marcarComoPagada();

        nominaPendiente = new Nomina(2L, trabajador,
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30),
                List.of(jornal));

        pagoBase = new Pago(1L, nominaPagada, LocalDate.now(),
                new BigDecimal("80000"), MetodoPago.TRANSFERENCIA, "COMP-001");
    }

    // ── registrarPago ─────────────────────────────────────────────────────

    @Test
    @DisplayName("registrarPago debe crear y guardar el pago cuando la nómina está PAGADA y sin pago previo")
    void registrarPago_datosValidos_debeCrearYGuardar() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPagada));
        when(pagoRepository.buscarPorNomina(1L)).thenReturn(Optional.empty());
        when(pagoRepository.guardar(any(Pago.class))).thenReturn(pagoBase);

        Pago resultado = pagoService.registrarPago(
                1L, new BigDecimal("80000"), MetodoPago.TRANSFERENCIA, "COMP-001", "Sin obs");

        assertNotNull(resultado);
        assertEquals(MetodoPago.TRANSFERENCIA, resultado.getMetodoPago());
        assertEquals("COMP-001", resultado.getComprobante());
        verify(pagoRepository).guardar(any(Pago.class));
    }

    @Test
    @DisplayName("registrarPago debe asignar las observaciones al pago")
    void registrarPago_debeAsignarObservaciones() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPagada));
        when(pagoRepository.buscarPorNomina(1L)).thenReturn(Optional.empty());
        when(pagoRepository.guardar(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago resultado = pagoService.registrarPago(
                1L, new BigDecimal("80000"), MetodoPago.EFECTIVO, "COMP-002", "Pago quincenal");

        assertEquals("Pago quincenal", resultado.getObservaciones());
    }

    @Test
    @DisplayName("registrarPago debe asignar fechaPago con la fecha actual")
    void registrarPago_debeAsignarFechaPagoHoy() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPagada));
        when(pagoRepository.buscarPorNomina(1L)).thenReturn(Optional.empty());
        when(pagoRepository.guardar(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago resultado = pagoService.registrarPago(
                1L, new BigDecimal("80000"), MetodoPago.CHEQUE, "COMP-003", "");

        assertEquals(LocalDate.now(), resultado.getFechaPago());
    }

    @Test
    @DisplayName("registrarPago debe lanzar IllegalArgumentException cuando la nómina no existe")
    void registrarPago_nominaInexistente_debeLanzarIllegalArgumentException() {
        when(nominaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.registrarPago(
                        99L, new BigDecimal("80000"), MetodoPago.EFECTIVO, "COMP", "")
        );

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(pagoRepository);
    }

    @Test
    @DisplayName("registrarPago debe lanzar IllegalStateException cuando la nómina no está en estado PAGADA")
    void registrarPago_nominaNoEnEstadoPagada_debeLanzarIllegalStateException() {
        when(nominaRepository.buscarPorId(2L)).thenReturn(Optional.of(nominaPendiente));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> pagoService.registrarPago(
                        2L, new BigDecimal("80000"), MetodoPago.EFECTIVO, "COMP", "")
        );

        assertTrue(ex.getMessage().contains("PAGADA"));
        verify(pagoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("registrarPago debe lanzar IllegalStateException cuando la nómina ya tiene un pago registrado")
    void registrarPago_nominaConPagoExistente_debeLanzarIllegalStateException() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPagada));
        when(pagoRepository.buscarPorNomina(1L)).thenReturn(Optional.of(pagoBase));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> pagoService.registrarPago(
                        1L, new BigDecimal("80000"), MetodoPago.EFECTIVO, "COMP", "")
        );

        assertTrue(ex.getMessage().contains("ya tiene un pago registrado"));
        verify(pagoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("registrarPago con observaciones null debe guardarse sin error")
    void registrarPago_observacionesNull_debeGuardarSinError() {
        when(nominaRepository.buscarPorId(1L)).thenReturn(Optional.of(nominaPagada));
        when(pagoRepository.buscarPorNomina(1L)).thenReturn(Optional.empty());
        when(pagoRepository.guardar(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago resultado = pagoService.registrarPago(
                1L, new BigDecimal("80000"), MetodoPago.TRANSFERENCIA, "COMP-004", null);

        assertNull(resultado.getObservaciones());
        verify(pagoRepository).guardar(any(Pago.class));
    }

    // ── buscarPorId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId debe retornar el pago cuando el id existe")
    void buscarPorId_idExiste_debeRetornarPago() {
        when(pagoRepository.buscarPorId(1L)).thenReturn(Optional.of(pagoBase));

        Pago resultado = pagoService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(MetodoPago.TRANSFERENCIA, resultado.getMetodoPago());
    }

    @Test
    @DisplayName("buscarPorId debe lanzar IllegalArgumentException cuando el id no existe")
    void buscarPorId_idInexistente_debeLanzarIllegalArgumentException() {
        when(pagoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.buscarPorId(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── historialPorTrabajador ────────────────────────────────────────────

    @Test
    @DisplayName("historialPorTrabajador debe retornar los pagos del trabajador")
    void historialPorTrabajador_debeRetornarLista() {
        when(pagoRepository.buscarPorTrabajador(1L)).thenReturn(List.of(pagoBase));

        List<Pago> resultado = pagoService.historialPorTrabajador(1L);

        assertEquals(1, resultado.size());
        verify(pagoRepository).buscarPorTrabajador(1L);
    }

    @Test
    @DisplayName("historialPorTrabajador debe retornar lista vacía cuando no hay pagos")
    void historialPorTrabajador_sinPagos_debeRetornarListaVacia() {
        when(pagoRepository.buscarPorTrabajador(1L)).thenReturn(List.of());

        assertTrue(pagoService.historialPorTrabajador(1L).isEmpty());
    }

    // ── listarTodos ───────────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos debe retornar todos los pagos registrados")
    void listarTodos_debeRetornarTodosLosPagos() {
        Pago pago2 = new Pago(2L, nominaPagada, LocalDate.now(),
                new BigDecimal("80000"), MetodoPago.EFECTIVO, "COMP-002");
        when(pagoRepository.listarTodos()).thenReturn(List.of(pagoBase, pago2));

        List<Pago> resultado = pagoService.listarTodos();

        assertEquals(2, resultado.size());
        verify(pagoRepository).listarTodos();
    }

    @Test
    @DisplayName("listarTodos debe retornar lista vacía cuando no hay pagos")
    void listarTodos_sinPagos_debeRetornarListaVacia() {
        when(pagoRepository.listarTodos()).thenReturn(List.of());

        assertTrue(pagoService.listarTodos().isEmpty());
    }
}