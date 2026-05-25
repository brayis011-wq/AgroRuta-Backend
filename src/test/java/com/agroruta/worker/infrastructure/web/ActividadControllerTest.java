package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.ActividadUseCase;
import com.agroruta.worker.domain.Actividad;
import com.agroruta.worker.infrastructure.web.dto.ActividadRequest;
import com.agroruta.worker.infrastructure.web.dto.ActividadResponse;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActividadController - Pruebas Unitarias")
class ActividadControllerTest {

    @Mock
    private ActividadUseCase actividadUseCase;

    @InjectMocks
    private ActividadController actividadController;

    private Actividad actividadBase;

    @BeforeEach
    void setUp() {
        actividadBase = new Actividad(1L, "Siembra", "Actividad de siembra");
    }

    private ActividadRequest request(String nombre, String descripcion) {
        ActividadRequest req = new ActividadRequest();
        req.nombre = nombre;
        req.descripcion = descripcion;
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET - listar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("Debe retornar 200 con la lista de actividades activas")
        void debeRetornar200ConLista() {
            List<Actividad> activas = List.of(
                    new Actividad(1L, "Siembra", "Actividad de siembra"),
                    new Actividad(2L, "Cosecha", "Actividad de cosecha")
            );
            when(actividadUseCase.listarActivas()).thenReturn(activas);

            ResponseEntity<List<ActividadResponse>> response = actividadController.listar();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().size());
            assertEquals("Siembra", response.getBody().get(0).nombre);
            assertEquals("Cosecha", response.getBody().get(1).nombre);
            verify(actividadUseCase, times(1)).listarActivas();
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si no hay actividades activas")
        void debeRetornar200ConListaVacia() {
            when(actividadUseCase.listarActivas()).thenReturn(List.of());

            ResponseEntity<List<ActividadResponse>> response = actividadController.listar();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());
            verify(actividadUseCase, times(1)).listarActivas();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST - crear
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("Debe retornar 201 con la actividad creada")
        void debeRetornar201AlCrear() {
            when(actividadUseCase.crearActividad("Siembra", "Actividad de siembra"))
                    .thenReturn(actividadBase);

            ResponseEntity<ActividadResponse> response =
                    actividadController.crear(request("Siembra", "Actividad de siembra"));

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().id);
            assertEquals("Siembra", response.getBody().nombre);
            assertEquals("Actividad de siembra", response.getBody().descripcion);
            assertTrue(response.getBody().activa);
            verify(actividadUseCase, times(1)).crearActividad("Siembra", "Actividad de siembra");
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el nombre ya existe")
        void debePropaglarExcepcionSiNombreDuplicado() {
            when(actividadUseCase.crearActividad(any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Siembra ya existe"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> actividadController.crear(request("Siembra", "Desc"))
            );

            assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Siembra"));
            verify(actividadUseCase, times(1)).crearActividad(any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUT - actualizar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        @Test
        @DisplayName("Debe retornar 200 con la actividad actualizada")
        void debeRetornar200AlActualizar() {
            Actividad actualizada = new Actividad(1L, "Cosecha", "Nueva descripción");
            when(actividadUseCase.actualizarActividad(1L, "Cosecha", "Nueva descripción"))
                    .thenReturn(actualizada);

            ResponseEntity<ActividadResponse> response =
                    actividadController.actualizar(1L, request("Cosecha", "Nueva descripción"));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().id);
            assertEquals("Cosecha", response.getBody().nombre);
            assertEquals("Nueva descripción", response.getBody().descripcion);
            verify(actividadUseCase, times(1)).actualizarActividad(1L, "Cosecha", "Nueva descripción");
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(actividadUseCase.actualizarActividad(eq(99L), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Actividad", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> actividadController.actualizar(99L, request("Cosecha", "Desc"))
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(actividadUseCase, times(1)).actualizarActividad(eq(99L), any(), any());
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el nuevo nombre ya existe en otra actividad")
        void debePropaglarExcepcionSiNombreDuplicadoEnOtra() {
            when(actividadUseCase.actualizarActividad(eq(1L), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Cosecha ya existe"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> actividadController.actualizar(1L, request("Cosecha", "Desc"))
            );

            assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
            verify(actividadUseCase, times(1)).actualizarActividad(eq(1L), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE - desactivar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("desactivar()")
    class Desactivar {

        @Test
        @DisplayName("Debe retornar 204 al desactivar correctamente")
        void debeRetornar204AlDesactivar() {
            doNothing().when(actividadUseCase).desactivarActividad(1L);

            ResponseEntity<Void> response = actividadController.desactivar(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(actividadUseCase, times(1)).desactivarActividad(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            doThrow(new ResourceNotFoundException("Actividad", 99L))
                    .when(actividadUseCase).desactivarActividad(99L);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> actividadController.desactivar(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(actividadUseCase, times(1)).desactivarActividad(99L);
        }
    }
}