package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.TrabajadorUseCase;
import com.agroruta.worker.domain.*;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorRequest;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorResponse;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorUpdateRequest;
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
@DisplayName("TrabajadorController - Pruebas Unitarias")
class TrabajadorControllerTest {

    @Mock
    private TrabajadorUseCase trabajadorUseCase;

    @InjectMocks
    private TrabajadorController trabajadorController;

    private Trabajador trabajadorBase;
    private Cargo cargoBase;

    private final LocalDate FECHA_INGRESO = LocalDate.of(2020, 1, 1);

    @BeforeEach
    void setUp() {
        cargoBase = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);
        trabajadorBase = new Trabajador(
                1L, "Juan", "Pérez", "123456789",
                "3001234567", "Calle 1", FECHA_INGRESO,
                TipoContrato.JORNAL, cargoBase
        );
    }

    private TrabajadorRequest buildRequest() {
        TrabajadorRequest req = new TrabajadorRequest();
        req.nombre       = "Juan";
        req.apellido     = "Pérez";
        req.cedula       = "123456789";
        req.telefono     = "3001234567";
        req.direccion    = "Calle 1";
        req.fechaIngreso = FECHA_INGRESO;
        req.tipoContrato = TipoContrato.JORNAL;
        req.cargoId      = 1L;
        return req;
    }

    private TrabajadorUpdateRequest buildUpdateRequest() {
        TrabajadorUpdateRequest req = new TrabajadorUpdateRequest();
        req.nombre       = "Juan";
        req.apellido     = "Pérez";
        req.telefono     = "3009999999";
        req.direccion    = "Calle 2";
        req.tipoContrato = TipoContrato.JORNAL;
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET - listar (activos)
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("Debe retornar 200 con la lista de trabajadores activos")
        void debeRetornar200ConTrabajadoresActivos() {
            when(trabajadorUseCase.listarActivos()).thenReturn(List.of(trabajadorBase));

            ResponseEntity<List<TrabajadorResponse>> response = trabajadorController.listar();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1,       response.getBody().size());
            assertEquals(1L,      response.getBody().get(0).id);
            assertEquals("Juan",  response.getBody().get(0).nombre);
            assertEquals("Pérez", response.getBody().get(0).apellido);
            verify(trabajadorUseCase, times(1)).listarActivos();
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si no hay trabajadores activos")
        void debeRetornar200ConListaVaciaDeActivos() {
            when(trabajadorUseCase.listarActivos()).thenReturn(List.of());

            ResponseEntity<List<TrabajadorResponse>> response = trabajadorController.listar();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
            verify(trabajadorUseCase, times(1)).listarActivos();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /todos - listarTodos
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Debe retornar 200 con todos los trabajadores")
        void debeRetornar200ConTodosLosTrabajadores() {
            when(trabajadorUseCase.listarTodos()).thenReturn(List.of(trabajadorBase));

            ResponseEntity<List<TrabajadorResponse>> response = trabajadorController.listarTodos();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1,  response.getBody().size());
            assertEquals(1L, response.getBody().get(0).id);
            verify(trabajadorUseCase, times(1)).listarTodos();
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si no existen trabajadores")
        void debeRetornar200ConListaVacia() {
            when(trabajadorUseCase.listarTodos()).thenReturn(List.of());

            ResponseEntity<List<TrabajadorResponse>> response = trabajadorController.listarTodos();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
            verify(trabajadorUseCase, times(1)).listarTodos();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /{id} - buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar 200 con el trabajador cuando el id existe")
        void debeRetornar200SiIdExiste() {
            when(trabajadorUseCase.buscarPorId(1L)).thenReturn(trabajadorBase);

            ResponseEntity<TrabajadorResponse> response = trabajadorController.buscarPorId(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,                  response.getBody().id);
            assertEquals("Juan",              response.getBody().nombre);
            assertEquals("Pérez",             response.getBody().apellido);
            assertEquals("123456789",         response.getBody().cedula);
            assertEquals("3001234567",        response.getBody().telefono);
            assertEquals(FECHA_INGRESO,       response.getBody().fechaIngreso);
            assertEquals(TipoContrato.JORNAL, response.getBody().tipoContrato);
            verify(trabajadorUseCase, times(1)).buscarPorId(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(trabajadorUseCase.buscarPorId(99L))
                    .thenThrow(new ResourceNotFoundException("Trabajador", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> trabajadorController.buscarPorId(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(trabajadorUseCase, times(1)).buscarPorId(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST - registrar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("Debe retornar 201 con el trabajador registrado")
        void debeRetornar201AlRegistrar() {
            when(trabajadorUseCase.registrarTrabajador(
                    "Juan", "Pérez", "123456789", "3001234567",
                    "Calle 1", FECHA_INGRESO, TipoContrato.JORNAL, 1L))
                    .thenReturn(trabajadorBase);

            ResponseEntity<TrabajadorResponse> response = trabajadorController.registrar(buildRequest());

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,          response.getBody().id);
            assertEquals("Juan",      response.getBody().nombre);
            assertEquals("Pérez",     response.getBody().apellido);
            assertEquals("123456789", response.getBody().cedula);
            verify(trabajadorUseCase, times(1)).registrarTrabajador(
                    "Juan", "Pérez", "123456789", "3001234567",
                    "Calle 1", FECHA_INGRESO, TipoContrato.JORNAL, 1L);
        }

        @Test
        @DisplayName("Debe propagar BusinessException si la cédula ya está registrada")
        void debePropaglarExcepcionSiCedulaDuplicada() {
            when(trabajadorUseCase.registrarTrabajador(
                    any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.BUSINESS_RULE_VIOLATION,
                            "Ya existe un trabajador con esa cédula"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> trabajadorController.registrar(buildRequest())
            );

            assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION, ex.getErrorCode());
            verify(trabajadorUseCase, times(1)).registrarTrabajador(
                    any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el cargo no existe")
        void debePropaglarExcepcionSiCargoNoExiste() {
            when(trabajadorUseCase.registrarTrabajador(
                    any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Cargo", 1L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> trabajadorController.registrar(buildRequest())
            );

            assertTrue(ex.getMessage().contains("1"));
            verify(trabajadorUseCase, times(1)).registrarTrabajador(
                    any(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUT /{id} - actualizar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        @Test
        @DisplayName("Debe retornar 200 con el trabajador actualizado")
        void debeRetornar200AlActualizar() {
            when(trabajadorUseCase.actualizarTrabajador(
                    1L, "Juan", "Pérez", "3009999999", "Calle 2", TipoContrato.JORNAL))
                    .thenReturn(trabajadorBase);

            ResponseEntity<TrabajadorResponse> response =
                    trabajadorController.actualizar(1L, buildUpdateRequest());

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,     response.getBody().id);
            assertEquals("Juan", response.getBody().nombre);
            verify(trabajadorUseCase, times(1)).actualizarTrabajador(
                    1L, "Juan", "Pérez", "3009999999", "Calle 2", TipoContrato.JORNAL);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el trabajador no existe")
        void debePropaglarExcepcionSiTrabajadorNoExiste() {
            when(trabajadorUseCase.actualizarTrabajador(
                    any(), any(), any(), any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Trabajador", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> trabajadorController.actualizar(99L, buildUpdateRequest())
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(trabajadorUseCase, times(1)).actualizarTrabajador(
                    any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el trabajador está inactivo")
        void debePropaglarExcepcionSiTrabajadorInactivo() {
            when(trabajadorUseCase.actualizarTrabajador(
                    any(), any(), any(), any(), any(), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.INVALID_OPERATION,
                            "No se puede actualizar un trabajador inactivo"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> trabajadorController.actualizar(1L, buildUpdateRequest())
            );

            assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
            verify(trabajadorUseCase, times(1)).actualizarTrabajador(
                    any(), any(), any(), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH /{id}/cargo/{cargoId} - cambiarCargo
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cambiarCargo()")
    class CambiarCargo {

        @Test
        @DisplayName("Debe retornar 200 con el trabajador asignado al nuevo cargo")
        void debeRetornar200AlCambiarCargo() {
            when(trabajadorUseCase.cambiarCargo(1L, 2L)).thenReturn(trabajadorBase);

            ResponseEntity<TrabajadorResponse> response = trabajadorController.cambiarCargo(1L, 2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().id);
            verify(trabajadorUseCase, times(1)).cambiarCargo(1L, 2L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el trabajador no existe")
        void debePropaglarExcepcionSiTrabajadorNoExiste() {
            when(trabajadorUseCase.cambiarCargo(99L, 2L))
                    .thenThrow(new ResourceNotFoundException("Trabajador", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> trabajadorController.cambiarCargo(99L, 2L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(trabajadorUseCase, times(1)).cambiarCargo(99L, 2L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el cargo no existe")
        void debePropaglarExcepcionSiCargoNoExiste() {
            when(trabajadorUseCase.cambiarCargo(1L, 99L))
                    .thenThrow(new ResourceNotFoundException("Cargo", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> trabajadorController.cambiarCargo(1L, 99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(trabajadorUseCase, times(1)).cambiarCargo(1L, 99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH /{id}/desactivar - desactivar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("desactivar()")
    class Desactivar {

        @Test
        @DisplayName("Debe retornar 204 al desactivar el trabajador exitosamente")
        void debeRetornar204AlDesactivar() {
            doNothing().when(trabajadorUseCase).desactivarTrabajador(1L);

            ResponseEntity<Void> response = trabajadorController.desactivar(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(trabajadorUseCase, times(1)).desactivarTrabajador(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el trabajador no existe")
        void debePropaglarExcepcionSiTrabajadorNoExiste() {
            doThrow(new ResourceNotFoundException("Trabajador", 99L))
                    .when(trabajadorUseCase).desactivarTrabajador(99L);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> trabajadorController.desactivar(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(trabajadorUseCase, times(1)).desactivarTrabajador(99L);
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el trabajador ya está inactivo")
        void debePropaglarExcepcionSiYaEstaInactivo() {
            doThrow(new BusinessException(
                    ErrorCode.INVALID_OPERATION, "El trabajador ya está inactivo"))
                    .when(trabajadorUseCase).desactivarTrabajador(1L);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> trabajadorController.desactivar(1L)
            );

            assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
            verify(trabajadorUseCase, times(1)).desactivarTrabajador(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH /{id}/suspender - suspender
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("suspender()")
    class Suspender {

        @Test
        @DisplayName("Debe retornar 204 al suspender el trabajador exitosamente")
        void debeRetornar204AlSuspender() {
            doNothing().when(trabajadorUseCase).suspenderTrabajador(1L);

            ResponseEntity<Void> response = trabajadorController.suspender(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(trabajadorUseCase, times(1)).suspenderTrabajador(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el trabajador no existe")
        void debePropaglarExcepcionSiTrabajadorNoExiste() {
            doThrow(new ResourceNotFoundException("Trabajador", 99L))
                    .when(trabajadorUseCase).suspenderTrabajador(99L);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> trabajadorController.suspender(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(trabajadorUseCase, times(1)).suspenderTrabajador(99L);
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el trabajador ya está suspendido")
        void debePropaglarExcepcionSiYaEstaSuspendido() {
            doThrow(new BusinessException(
                    ErrorCode.INVALID_OPERATION, "El trabajador ya está suspendido"))
                    .when(trabajadorUseCase).suspenderTrabajador(1L);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> trabajadorController.suspender(1L)
            );

            assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
            verify(trabajadorUseCase, times(1)).suspenderTrabajador(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH /{id}/reactivar - reactivar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("reactivar()")
    class Reactivar {

        @Test
        @DisplayName("Debe retornar 204 al reactivar el trabajador exitosamente")
        void debeRetornar204AlReactivar() {
            doNothing().when(trabajadorUseCase).reactivarTrabajador(1L);

            ResponseEntity<Void> response = trabajadorController.reactivar(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(trabajadorUseCase, times(1)).reactivarTrabajador(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el trabajador no existe")
        void debePropaglarExcepcionSiTrabajadorNoExiste() {
            doThrow(new ResourceNotFoundException("Trabajador", 99L))
                    .when(trabajadorUseCase).reactivarTrabajador(99L);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> trabajadorController.reactivar(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(trabajadorUseCase, times(1)).reactivarTrabajador(99L);
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el trabajador ya está activo")
        void debePropaglarExcepcionSiYaEstaActivo() {
            doThrow(new BusinessException(
                    ErrorCode.INVALID_OPERATION, "El trabajador ya está activo"))
                    .when(trabajadorUseCase).reactivarTrabajador(1L);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> trabajadorController.reactivar(1L)
            );

            assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
            verify(trabajadorUseCase, times(1)).reactivarTrabajador(1L);
        }
    }
}