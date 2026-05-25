package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.PagoUseCase;
import com.agroruta.worker.domain.*;
import com.agroruta.worker.infrastructure.web.dto.PagoRequest;
import com.agroruta.worker.infrastructure.web.dto.PagoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoController - Pruebas Unitarias")
class PagoControllerTest {

    @Mock
    private PagoUseCase pagoUseCase;

    @InjectMocks
    private PagoController pagoController;

    private Pago pagoBase;
    private Nomina nominaBase;
    private Trabajador trabajadorBase;
    private Cargo cargoBase;

    private final LocalDate FECHA_PAGO = LocalDate.of(2025, 2, 1);
    private final LocalDate INICIO     = LocalDate.of(2025, 1, 1);
    private final LocalDate FIN        = LocalDate.of(2025, 1, 31);
    private final LocalDate FECHA      = LocalDate.of(2025, 1, 15);

    @BeforeEach
    void setUp() {
        cargoBase = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);

        trabajadorBase = new Trabajador(
                1L, "Juan", "Pérez", "123456789",
                "3001234567", "Calle 1", LocalDate.of(2020, 1, 1),
                TipoContrato.JORNAL, cargoBase
        );

        Jornal jornal = new Jornal(1L, FECHA, trabajadorBase, 10L, "Maíz", "Sin observaciones");
        nominaBase = new Nomina(1L, trabajadorBase, INICIO, FIN, List.of(jornal));

        pagoBase = new Pago(
                1L, nominaBase, FECHA_PAGO,
                new BigDecimal("80000"), MetodoPago.TRANSFERENCIA, "COMP-001"
        );
        pagoBase.setObservaciones("Pago mensual");
    }

    private PagoRequest request() {
        PagoRequest req = new PagoRequest();
        req.nominaId     = 1L;
        req.monto        = new BigDecimal("80000");
        req.metodoPago   = MetodoPago.TRANSFERENCIA;
        req.comprobante  = "COMP-001";
        req.observaciones = "Pago mensual";
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST - registrar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("Debe retornar 201 con el pago registrado")
        void debeRetornar201AlRegistrar() {
            when(pagoUseCase.registrarPago(
                    1L, new BigDecimal("80000"), MetodoPago.TRANSFERENCIA, "COMP-001", "Pago mensual"))
                    .thenReturn(pagoBase);

            ResponseEntity<PagoResponse> response = pagoController.registrar(request());

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,                      response.getBody().id);
            assertEquals(1L,                      response.getBody().nominaId);
            assertEquals(1L,                      response.getBody().trabajadorId);
            assertEquals("Juan Pérez",            response.getBody().trabajadorNombre);
            assertEquals(FECHA_PAGO,              response.getBody().fechaPago);
            assertEquals(new BigDecimal("80000"), response.getBody().monto);
            assertEquals(MetodoPago.TRANSFERENCIA, response.getBody().metodoPago);
            assertEquals("COMP-001",              response.getBody().comprobante);
            assertEquals("Pago mensual",          response.getBody().observaciones);
            verify(pagoUseCase, times(1)).registrarPago(
                    1L, new BigDecimal("80000"), MetodoPago.TRANSFERENCIA, "COMP-001", "Pago mensual");
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si la nómina no existe")
        void debePropaglarExcepcionSiNominaNoExiste() {
            when(pagoUseCase.registrarPago(any(), any(), any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Nomina", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> pagoController.registrar(request())
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(pagoUseCase, times(1)).registrarPago(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el monto no coincide con el valor de la nómina")
        void debePropaglarExcepcionSiMontoIncorrecto() {
            when(pagoUseCase.registrarPago(any(), any(), any(), any(), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.BUSINESS_RULE_VIOLATION,
                            "El monto no coincide con el valor total de la nómina"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> pagoController.registrar(request())
            );

            assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION, ex.getErrorCode());
            verify(pagoUseCase, times(1)).registrarPago(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Debe propagar BusinessException si la nómina no está aprobada")
        void debePropaglarExcepcionSiNominaNoAprobada() {
            when(pagoUseCase.registrarPago(any(), any(), any(), any(), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.INVALID_OPERATION,
                            "Solo se pueden pagar nóminas en estado APROBADA"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> pagoController.registrar(request())
            );

            assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
            verify(pagoUseCase, times(1)).registrarPago(any(), any(), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /{id} - buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar 200 con el pago cuando el id existe")
        void debeRetornar200SiIdExiste() {
            when(pagoUseCase.buscarPorId(1L)).thenReturn(pagoBase);

            ResponseEntity<PagoResponse> response = pagoController.buscarPorId(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,                       response.getBody().id);
            assertEquals(MetodoPago.TRANSFERENCIA, response.getBody().metodoPago);
            assertEquals("COMP-001",               response.getBody().comprobante);
            verify(pagoUseCase, times(1)).buscarPorId(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(pagoUseCase.buscarPorId(99L))
                    .thenThrow(new ResourceNotFoundException("Pago", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> pagoController.buscarPorId(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(pagoUseCase, times(1)).buscarPorId(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /trabajador/{trabajadorId} - historialPorTrabajador
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("historialPorTrabajador()")
    class HistorialPorTrabajador {

        @Test
        @DisplayName("Debe retornar 200 con el historial de pagos del trabajador")
        void debeRetornar200ConHistorial() {
            when(pagoUseCase.historialPorTrabajador(1L)).thenReturn(List.of(pagoBase));

            ResponseEntity<List<PagoResponse>> response = pagoController.historialPorTrabajador(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals(1L, response.getBody().get(0).trabajadorId);
            verify(pagoUseCase, times(1)).historialPorTrabajador(1L);
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si el trabajador no tiene pagos")
        void debeRetornar200ConListaVacia() {
            when(pagoUseCase.historialPorTrabajador(1L)).thenReturn(List.of());

            ResponseEntity<List<PagoResponse>> response = pagoController.historialPorTrabajador(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
            verify(pagoUseCase, times(1)).historialPorTrabajador(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el trabajador no existe")
        void debePropaglarExcepcionSiTrabajadorNoExiste() {
            when(pagoUseCase.historialPorTrabajador(99L))
                    .thenThrow(new ResourceNotFoundException("Trabajador", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> pagoController.historialPorTrabajador(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(pagoUseCase, times(1)).historialPorTrabajador(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET - listarTodos
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Debe retornar 200 con todos los pagos")
        void debeRetornar200ConTodosLosPagos() {
            when(pagoUseCase.listarTodos()).thenReturn(List.of(pagoBase));

            ResponseEntity<List<PagoResponse>> response = pagoController.listarTodos();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(pagoUseCase, times(1)).listarTodos();
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si no hay pagos")
        void debeRetornar200ConListaVacia() {
            when(pagoUseCase.listarTodos()).thenReturn(List.of());

            ResponseEntity<List<PagoResponse>> response = pagoController.listarTodos();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
            verify(pagoUseCase, times(1)).listarTodos();
        }
    }
}