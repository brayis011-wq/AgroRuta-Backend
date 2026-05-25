package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.CargoUseCase;
import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.infrastructure.web.dto.CargoRequest;
import com.agroruta.worker.infrastructure.web.dto.CargoResponse;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CargoController - Pruebas Unitarias")
class CargoControllerTest {

    @Mock
    private CargoUseCase cargoUseCase;

    @InjectMocks
    private CargoController cargoController;

    private Cargo cargoBase;

    @BeforeEach
    void setUp() {
        cargoBase = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);
    }

    private CargoRequest request(String nombre, String descripcion, BigDecimal valorJornal) {
        CargoRequest req = new CargoRequest();
        req.nombre = nombre;
        req.descripcion = descripcion;
        req.valorJornal = valorJornal;
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET - listar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("Debe retornar 200 con la lista de cargos activos")
        void debeRetornar200ConLista() {
            List<Cargo> activos = List.of(
                    new Cargo(1L, "Operario",  "Desc 1", new BigDecimal("80000"),  true),
                    new Cargo(2L, "Supervisor", "Desc 2", new BigDecimal("120000"), true)
            );
            when(cargoUseCase.listarActivos()).thenReturn(activos);

            ResponseEntity<List<CargoResponse>> response = cargoController.listar();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().size());
            assertEquals("Operario",   response.getBody().get(0).nombre);
            assertEquals("Supervisor", response.getBody().get(1).nombre);
            verify(cargoUseCase, times(1)).listarActivos();
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si no hay cargos activos")
        void debeRetornar200ConListaVacia() {
            when(cargoUseCase.listarActivos()).thenReturn(List.of());

            ResponseEntity<List<CargoResponse>> response = cargoController.listar();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());
            verify(cargoUseCase, times(1)).listarActivos();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /{id} - buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar 200 con el cargo cuando el id existe")
        void debeRetornar200SiIdExiste() {
            when(cargoUseCase.buscarPorId(1L)).thenReturn(cargoBase);

            ResponseEntity<CargoResponse> response = cargoController.buscarPorId(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,                       response.getBody().id);
            assertEquals("Operario",               response.getBody().nombre);
            assertEquals("Cargo operario",         response.getBody().descripcion);
            assertEquals(new BigDecimal("80000"),  response.getBody().valorJornal);
            assertTrue(response.getBody().activo);
            verify(cargoUseCase, times(1)).buscarPorId(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(cargoUseCase.buscarPorId(99L))
                    .thenThrow(new ResourceNotFoundException("Cargo", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> cargoController.buscarPorId(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(cargoUseCase, times(1)).buscarPorId(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST - crear
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("Debe retornar 201 con el cargo creado")
        void debeRetornar201AlCrear() {
            when(cargoUseCase.crearCargo("Operario", "Cargo operario", new BigDecimal("80000")))
                    .thenReturn(cargoBase);

            ResponseEntity<CargoResponse> response = cargoController.crear(
                    request("Operario", "Cargo operario", new BigDecimal("80000"))
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,                      response.getBody().id);
            assertEquals("Operario",              response.getBody().nombre);
            assertEquals("Cargo operario",        response.getBody().descripcion);
            assertEquals(new BigDecimal("80000"), response.getBody().valorJornal);
            assertTrue(response.getBody().activo);
            verify(cargoUseCase, times(1))
                    .crearCargo("Operario", "Cargo operario", new BigDecimal("80000"));
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el nombre ya existe")
        void debePropaglarExcepcionSiNombreDuplicado() {
            when(cargoUseCase.crearCargo(any(), any(), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.RESOURCE_ALREADY_EXISTS, "Operario ya existe"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> cargoController.crear(
                            request("Operario", "Desc", new BigDecimal("80000")))
            );

            assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Operario"));
            verify(cargoUseCase, times(1)).crearCargo(any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUT /{id} - actualizar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        @Test
        @DisplayName("Debe retornar 200 con el cargo actualizado")
        void debeRetornar200AlActualizar() {
            Cargo actualizado = new Cargo(1L, "Supervisor", "Nuevo desc", new BigDecimal("120000"), true);
            when(cargoUseCase.actualizarCargo(1L, "Supervisor", "Nuevo desc", new BigDecimal("120000")))
                    .thenReturn(actualizado);

            ResponseEntity<CargoResponse> response = cargoController.actualizar(
                    1L, request("Supervisor", "Nuevo desc", new BigDecimal("120000"))
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,                        response.getBody().id);
            assertEquals("Supervisor",              response.getBody().nombre);
            assertEquals("Nuevo desc",              response.getBody().descripcion);
            assertEquals(new BigDecimal("120000"),  response.getBody().valorJornal);
            verify(cargoUseCase, times(1))
                    .actualizarCargo(1L, "Supervisor", "Nuevo desc", new BigDecimal("120000"));
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(cargoUseCase.actualizarCargo(eq(99L), any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Cargo", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> cargoController.actualizar(
                            99L, request("Supervisor", "Desc", new BigDecimal("120000")))
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(cargoUseCase, times(1)).actualizarCargo(eq(99L), any(), any(), any());
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el nuevo nombre ya existe en otro cargo")
        void debePropaglarExcepcionSiNombreDuplicadoEnOtro() {
            when(cargoUseCase.actualizarCargo(eq(1L), any(), any(), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.RESOURCE_ALREADY_EXISTS, "Supervisor ya existe"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> cargoController.actualizar(
                            1L, request("Supervisor", "Desc", new BigDecimal("120000")))
            );

            assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
            verify(cargoUseCase, times(1)).actualizarCargo(eq(1L), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE /{id} - desactivar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("desactivar()")
    class Desactivar {

        @Test
        @DisplayName("Debe retornar 204 al desactivar correctamente")
        void debeRetornar204AlDesactivar() {
            doNothing().when(cargoUseCase).desactivarCargo(1L);

            ResponseEntity<Void> response = cargoController.desactivar(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(cargoUseCase, times(1)).desactivarCargo(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            doThrow(new ResourceNotFoundException("Cargo", 99L))
                    .when(cargoUseCase).desactivarCargo(99L);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> cargoController.desactivar(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(cargoUseCase, times(1)).desactivarCargo(99L);
        }
    }
}