package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.NominaUseCase;
import com.agroruta.worker.domain.*;
import com.agroruta.worker.infrastructure.web.dto.NominaRequest;
import com.agroruta.worker.infrastructure.web.dto.NominaResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NominaController - Pruebas Unitarias")
class NominaControllerTest {

    @Mock
    private NominaUseCase nominaUseCase;

    @InjectMocks
    private NominaController nominaController;

    private Nomina nominaBase;
    private Trabajador trabajadorBase;
    private Cargo cargoBase;

    private final LocalDate INICIO = LocalDate.of(2025, 1, 1);
    private final LocalDate FIN    = LocalDate.of(2025, 1, 31);
    private final LocalDate FECHA  = LocalDate.of(2025, 1, 15);

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
    }

    private NominaRequest request() {
        NominaRequest req = new NominaRequest();
        req.trabajadorId  = 1L;
        req.periodoInicio = INICIO;
        req.periodoFin    = FIN;
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST /generar - generar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("generar()")
    class Generar {

        @Test
        @DisplayName("Debe retornar 201 con la nómina generada")
        void debeRetornar201AlGenerar() {
            when(nominaUseCase.generarNomina(1L, INICIO, FIN)).thenReturn(nominaBase);

            ResponseEntity<NominaResponse> response = nominaController.generar(request());

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,                    response.getBody().id);
            assertEquals(INICIO,                response.getBody().periodoInicio);
            assertEquals(FIN,                   response.getBody().periodoFin);
            assertEquals(1,                     response.getBody().totalJornales);
            assertEquals(new BigDecimal("80000"), response.getBody().valorTotal);
            assertEquals(EstadoNomina.PENDIENTE, response.getBody().estado);
            verify(nominaUseCase, times(1)).generarNomina(1L, INICIO, FIN);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el trabajador no existe")
        void debePropaglarExcepcionSiTrabajadorNoExiste() {
            when(nominaUseCase.generarNomina(any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Trabajador", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> nominaController.generar(request())
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(nominaUseCase, times(1)).generarNomina(any(), any(), any());
        }

        @Test
        @DisplayName("Debe propagar BusinessException si no hay jornales en el periodo")
        void debePropaglarExcepcionSiNoHayJornales() {
            when(nominaUseCase.generarNomina(any(), any(), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.BUSINESS_RULE_VIOLATION,
                            "No hay jornales en el periodo indicado"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> nominaController.generar(request())
            );

            assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION, ex.getErrorCode());
            verify(nominaUseCase, times(1)).generarNomina(any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /{id} - buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar 200 con la nómina cuando el id existe")
        void debeRetornar200SiIdExiste() {
            when(nominaUseCase.buscarPorId(1L)).thenReturn(nominaBase);

            ResponseEntity<NominaResponse> response = nominaController.buscarPorId(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,    response.getBody().id);
            assertEquals(INICIO, response.getBody().periodoInicio);
            assertEquals(FIN,    response.getBody().periodoFin);
            verify(nominaUseCase, times(1)).buscarPorId(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(nominaUseCase.buscarPorId(99L))
                    .thenThrow(new ResourceNotFoundException("Nomina", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> nominaController.buscarPorId(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(nominaUseCase, times(1)).buscarPorId(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /trabajador/{trabajadorId} - porTrabajador
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("porTrabajador()")
    class PorTrabajador {

        @Test
        @DisplayName("Debe retornar 200 con la lista de nóminas del trabajador")
        void debeRetornar200ConLista() {
            when(nominaUseCase.listarPorTrabajador(1L)).thenReturn(List.of(nominaBase));

            ResponseEntity<List<NominaResponse>> response = nominaController.porTrabajador(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(nominaUseCase, times(1)).listarPorTrabajador(1L);
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si el trabajador no tiene nóminas")
        void debeRetornar200ConListaVacia() {
            when(nominaUseCase.listarPorTrabajador(1L)).thenReturn(List.of());

            ResponseEntity<List<NominaResponse>> response = nominaController.porTrabajador(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
            verify(nominaUseCase, times(1)).listarPorTrabajador(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET - listarTodas
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarTodas()")
    class ListarTodas {

        @Test
        @DisplayName("Debe retornar 200 con todas las nóminas")
        void debeRetornar200ConTodasLasNominas() {
            when(nominaUseCase.listarTodas()).thenReturn(List.of(nominaBase));

            ResponseEntity<List<NominaResponse>> response = nominaController.listarTodas();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(nominaUseCase, times(1)).listarTodas();
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si no hay nóminas")
        void debeRetornar200ConListaVacia() {
            when(nominaUseCase.listarTodas()).thenReturn(List.of());

            ResponseEntity<List<NominaResponse>> response = nominaController.listarTodas();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
            verify(nominaUseCase, times(1)).listarTodas();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH /{id}/aprobar - aprobar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("aprobar()")
    class Aprobar {

        @Test
        @DisplayName("Debe retornar 200 con la nómina aprobada")
        void debeRetornar200AlAprobar() {
            nominaBase.aprobar();
            when(nominaUseCase.aprobarNomina(1L)).thenReturn(nominaBase);

            ResponseEntity<NominaResponse> response = nominaController.aprobar(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(EstadoNomina.APROBADA, response.getBody().estado);
            verify(nominaUseCase, times(1)).aprobarNomina(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(nominaUseCase.aprobarNomina(99L))
                    .thenThrow(new ResourceNotFoundException("Nomina", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> nominaController.aprobar(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(nominaUseCase, times(1)).aprobarNomina(99L);
        }

        @Test
        @DisplayName("Debe propagar IllegalStateException si la nómina no está en estado PENDIENTE")
        void debePropaglarExcepcionSiEstadoInvalido() {
            when(nominaUseCase.aprobarNomina(1L))
                    .thenThrow(new IllegalStateException(
                            "Solo se pueden aprobar nóminas en estado PENDIENTE."));

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> nominaController.aprobar(1L)
            );

            assertTrue(ex.getMessage().contains("PENDIENTE"));
            verify(nominaUseCase, times(1)).aprobarNomina(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH /{id}/anular - anular
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("anular()")
    class Anular {

        @Test
        @DisplayName("Debe retornar 204 al anular correctamente")
        void debeRetornar204AlAnular() {
            doNothing().when(nominaUseCase).anularNomina(1L);

            ResponseEntity<Void> response = nominaController.anular(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(nominaUseCase, times(1)).anularNomina(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            doThrow(new ResourceNotFoundException("Nomina", 99L))
                    .when(nominaUseCase).anularNomina(99L);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> nominaController.anular(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(nominaUseCase, times(1)).anularNomina(99L);
        }

        @Test
        @DisplayName("Debe propagar IllegalStateException si la nómina ya está pagada")
        void debePropaglarExcepcionSiNominaYaPagada() {
            doThrow(new IllegalStateException("No se puede anular una nómina ya pagada."))
                    .when(nominaUseCase).anularNomina(1L);

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> nominaController.anular(1L)
            );

            assertTrue(ex.getMessage().contains("pagada"));
            verify(nominaUseCase, times(1)).anularNomina(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH /{id}/reactivar - reactivar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("reactivar()")
    class Reactivar {

        @Test
        @DisplayName("Debe retornar 200 con la nómina reactivada a PENDIENTE")
        void debeRetornar200AlReactivar() {
            nominaBase.anular();
            nominaBase.reactivar();
            when(nominaUseCase.reactivarNomina(1L)).thenReturn(nominaBase);

            ResponseEntity<NominaResponse> response = nominaController.reactivar(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(EstadoNomina.PENDIENTE, response.getBody().estado);
            verify(nominaUseCase, times(1)).reactivarNomina(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(nominaUseCase.reactivarNomina(99L))
                    .thenThrow(new ResourceNotFoundException("Nomina", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> nominaController.reactivar(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(nominaUseCase, times(1)).reactivarNomina(99L);
        }

        @Test
        @DisplayName("Debe propagar IllegalStateException si la nómina no está ANULADA")
        void debePropaglarExcepcionSiEstadoInvalido() {
            when(nominaUseCase.reactivarNomina(1L))
                    .thenThrow(new IllegalStateException(
                            "Solo se pueden reactivar nóminas en estado ANULADA."));

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> nominaController.reactivar(1L)
            );

            assertTrue(ex.getMessage().contains("ANULADA"));
            verify(nominaUseCase, times(1)).reactivarNomina(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE /{id} - eliminar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        @Test
        @DisplayName("Debe retornar 204 al eliminar correctamente")
        void debeRetornar204AlEliminar() {
            doNothing().when(nominaUseCase).eliminarNomina(1L);

            ResponseEntity<Void> response = nominaController.eliminar(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(nominaUseCase, times(1)).eliminarNomina(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            doThrow(new ResourceNotFoundException("Nomina", 99L))
                    .when(nominaUseCase).eliminarNomina(99L);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> nominaController.eliminar(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(nominaUseCase, times(1)).eliminarNomina(99L);
        }

        @Test
        @DisplayName("Debe propagar BusinessException si la nómina no se puede eliminar")
        void debePropaglarExcepcionPorReglaNegocio() {
            doThrow(new BusinessException(
                    ErrorCode.INVALID_OPERATION, "No se puede eliminar una nómina aprobada"))
                    .when(nominaUseCase).eliminarNomina(1L);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> nominaController.eliminar(1L)
            );

            assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
            verify(nominaUseCase, times(1)).eliminarNomina(1L);
        }
    }
}