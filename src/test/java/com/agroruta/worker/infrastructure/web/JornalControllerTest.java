package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.JornalUseCase;
import com.agroruta.worker.domain.Actividad;
import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.domain.EstadoTrabajador;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.TipoContrato;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.infrastructure.web.dto.JornalRequest;
import com.agroruta.worker.infrastructure.web.dto.JornalResponse;
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
@DisplayName("JornalController - Pruebas Unitarias")
class JornalControllerTest {

    @Mock
    private JornalUseCase jornalUseCase;

    @InjectMocks
    private JornalController jornalController;

    private Jornal jornalBase;
    private Trabajador trabajadorBase;
    private Cargo cargoBase;
    private final LocalDate FECHA = LocalDate.of(2025, 1, 15);

    @BeforeEach
    void setUp() {
        cargoBase = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);

        trabajadorBase = new Trabajador(
                1L, "Juan", "Pérez", "123456789",
                "3001234567", "Calle 1", LocalDate.of(2020, 1, 1),
                TipoContrato.JORNAL, cargoBase
        );

        jornalBase = new Jornal(1L, FECHA, trabajadorBase, 10L, "Maíz", "Sin observaciones");
    }

    private JornalRequest request() {
        JornalRequest req = new JornalRequest();
        req.trabajadorId  = 1L;
        req.cultivoId     = 10L;
        req.nombreCultivo = "Maíz";
        req.fecha         = FECHA;
        req.actividadIds  = List.of(1L, 2L);
        req.observaciones = "Sin observaciones";
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST - registrar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("Debe retornar 201 con el jornal registrado")
        void debeRetornar201AlRegistrar() {
            when(jornalUseCase.registrarJornal(
                    eq(1L), eq(10L), eq("Maíz"),
                    eq(FECHA), eq(List.of(1L, 2L)), eq("Sin observaciones")))
                    .thenReturn(jornalBase);

            ResponseEntity<JornalResponse> response = jornalController.registrar(request());

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,     response.getBody().id);
            assertEquals(FECHA,  response.getBody().fecha);
            assertEquals(10L,    response.getBody().cultivoId);
            assertEquals("Maíz", response.getBody().nombreCultivo);
            assertEquals(new BigDecimal("80000"), response.getBody().valorJornal);
            assertFalse(response.getBody().liquidado);
            verify(jornalUseCase, times(1)).registrarJornal(
                    eq(1L), eq(10L), eq("Maíz"),
                    eq(FECHA), eq(List.of(1L, 2L)), eq("Sin observaciones"));
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el trabajador no existe")
        void debePropaglarExcepcionSiTrabajadorNoExiste() {
            when(jornalUseCase.registrarJornal(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Trabajador", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> jornalController.registrar(request())
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(jornalUseCase, times(1)).registrarJornal(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el jornal viola una regla de negocio")
        void debePropaglarExcepcionPorReglaNegocio() {
            when(jornalUseCase.registrarJornal(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.BUSINESS_RULE_VIOLATION, "El trabajador ya tiene un jornal en esa fecha"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> jornalController.registrar(request())
            );

            assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION, ex.getErrorCode());
            verify(jornalUseCase, times(1)).registrarJornal(any(), any(), any(), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /{id} - buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar 200 con el jornal cuando el id existe")
        void debeRetornar200SiIdExiste() {
            when(jornalUseCase.buscarPorId(1L)).thenReturn(jornalBase);

            ResponseEntity<JornalResponse> response = jornalController.buscarPorId(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L,    response.getBody().id);
            assertEquals(FECHA, response.getBody().fecha);
            assertEquals("Sin observaciones", response.getBody().observaciones);
            verify(jornalUseCase, times(1)).buscarPorId(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(jornalUseCase.buscarPorId(99L))
                    .thenThrow(new ResourceNotFoundException("Jornal", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> jornalController.buscarPorId(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(jornalUseCase, times(1)).buscarPorId(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /trabajador/{trabajadorId} - porTrabajador
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("porTrabajador()")
    class PorTrabajador {

        @Test
        @DisplayName("Debe retornar 200 con todos los jornales del trabajador sin filtro de fechas")
        void debeRetornar200SinFiltroFechas() {
            when(jornalUseCase.listarPorTrabajador(1L)).thenReturn(List.of(jornalBase));

            ResponseEntity<List<JornalResponse>> response =
                    jornalController.porTrabajador(1L, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(jornalUseCase, times(1)).listarPorTrabajador(1L);
            verify(jornalUseCase, never()).listarPorTrabajadorYPeriodo(any(), any(), any());
        }

        @Test
        @DisplayName("Debe retornar 200 filtrando por periodo cuando se pasan inicio y fin")
        void debeRetornar200ConFiltroPeriodo() {
            LocalDate inicio = LocalDate.of(2025, 1, 1);
            LocalDate fin    = LocalDate.of(2025, 1, 31);
            when(jornalUseCase.listarPorTrabajadorYPeriodo(1L, inicio, fin))
                    .thenReturn(List.of(jornalBase));

            ResponseEntity<List<JornalResponse>> response =
                    jornalController.porTrabajador(1L, inicio, fin);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(jornalUseCase, times(1)).listarPorTrabajadorYPeriodo(1L, inicio, fin);
            verify(jornalUseCase, never()).listarPorTrabajador(any());
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si el trabajador no tiene jornales")
        void debeRetornar200ConListaVacia() {
            when(jornalUseCase.listarPorTrabajador(1L)).thenReturn(List.of());

            ResponseEntity<List<JornalResponse>> response =
                    jornalController.porTrabajador(1L, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /cultivo/{cultivoId} - porCultivo
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("porCultivo()")
    class PorCultivo {

        @Test
        @DisplayName("Debe retornar 200 con todos los jornales del cultivo sin filtro de fechas")
        void debeRetornar200SinFiltroFechas() {
            when(jornalUseCase.listarPorCultivo(10L)).thenReturn(List.of(jornalBase));

            ResponseEntity<List<JornalResponse>> response =
                    jornalController.porCultivo(10L, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(jornalUseCase, times(1)).listarPorCultivo(10L);
            verify(jornalUseCase, never()).listarPorCultivoYPeriodo(any(), any(), any());
        }

        @Test
        @DisplayName("Debe retornar 200 filtrando por periodo cuando se pasan inicio y fin")
        void debeRetornar200ConFiltroPeriodo() {
            LocalDate inicio = LocalDate.of(2025, 1, 1);
            LocalDate fin    = LocalDate.of(2025, 1, 31);
            when(jornalUseCase.listarPorCultivoYPeriodo(10L, inicio, fin))
                    .thenReturn(List.of(jornalBase));

            ResponseEntity<List<JornalResponse>> response =
                    jornalController.porCultivo(10L, inicio, fin);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(jornalUseCase, times(1)).listarPorCultivoYPeriodo(10L, inicio, fin);
            verify(jornalUseCase, never()).listarPorCultivo(any());
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si el cultivo no tiene jornales")
        void debeRetornar200ConListaVacia() {
            when(jornalUseCase.listarPorCultivo(10L)).thenReturn(List.of());

            ResponseEntity<List<JornalResponse>> response =
                    jornalController.porCultivo(10L, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH /{id}/actividades/{actividadId} - agregarActividad
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("agregarActividad()")
    class AgregarActividad {

        @Test
        @DisplayName("Debe retornar 200 con el jornal actualizado al agregar actividad")
        void debeRetornar200AlAgregarActividad() {
            Actividad actividad = new Actividad(2L, "Riego", "Actividad de riego");
            jornalBase.agregarActividad(actividad);
            when(jornalUseCase.agregarActividad(1L, 2L)).thenReturn(jornalBase);

            ResponseEntity<JornalResponse> response =
                    jornalController.agregarActividad(1L, 2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().actividades.size());
            assertEquals("Riego", response.getBody().actividades.get(0).nombre);
            verify(jornalUseCase, times(1)).agregarActividad(1L, 2L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el jornal no existe")
        void debePropaglarExcepcionSiJornalNoExiste() {
            when(jornalUseCase.agregarActividad(99L, 2L))
                    .thenThrow(new ResourceNotFoundException("Jornal", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> jornalController.agregarActividad(99L, 2L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(jornalUseCase, times(1)).agregarActividad(99L, 2L);
        }

        @Test
        @DisplayName("Debe propagar IllegalStateException si el jornal ya está liquidado")
        void debePropaglarExcepcionSiJornalLiquidado() {
            when(jornalUseCase.agregarActividad(1L, 2L))
                    .thenThrow(new IllegalStateException(
                            "No se pueden modificar actividades de un jornal ya liquidado."));

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> jornalController.agregarActividad(1L, 2L)
            );

            assertTrue(ex.getMessage().contains("liquidado"));
            verify(jornalUseCase, times(1)).agregarActividad(1L, 2L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE /{id}/actividades/{actividadId} - removerActividad
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("removerActividad()")
    class RemoverActividad {

        @Test
        @DisplayName("Debe retornar 200 con el jornal actualizado al remover actividad")
        void debeRetornar200AlRemoverActividad() {
            when(jornalUseCase.removerActividad(1L, 2L)).thenReturn(jornalBase);

            ResponseEntity<JornalResponse> response =
                    jornalController.removerActividad(1L, 2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().actividades.isEmpty());
            verify(jornalUseCase, times(1)).removerActividad(1L, 2L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el jornal no existe")
        void debePropaglarExcepcionSiJornalNoExiste() {
            when(jornalUseCase.removerActividad(99L, 2L))
                    .thenThrow(new ResourceNotFoundException("Jornal", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> jornalController.removerActividad(99L, 2L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(jornalUseCase, times(1)).removerActividad(99L, 2L);
        }

        @Test
        @DisplayName("Debe propagar IllegalStateException si el jornal ya está liquidado")
        void debePropaglarExcepcionSiJornalLiquidado() {
            when(jornalUseCase.removerActividad(1L, 2L))
                    .thenThrow(new IllegalStateException(
                            "No se pueden modificar actividades de un jornal ya liquidado."));

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> jornalController.removerActividad(1L, 2L)
            );

            assertTrue(ex.getMessage().contains("liquidado"));
            verify(jornalUseCase, times(1)).removerActividad(1L, 2L);
        }
    }
}