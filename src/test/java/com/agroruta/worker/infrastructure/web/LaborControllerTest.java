package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.LaborUseCase;
import com.agroruta.worker.domain.Labor;
import com.agroruta.worker.infrastructure.web.dto.LaborRequest;
import com.agroruta.worker.infrastructure.web.dto.LaborResponse;
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
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LaborController - Pruebas Unitarias")
class LaborControllerTest {

    @Mock
    private LaborUseCase laborUseCase;

    @InjectMocks
    private LaborController actividadController;

    private Labor actividadBase;

    @BeforeEach
    void setUp() {
        actividadBase = new Labor(1L, "Siembra", "Labor de siembra");
    }

    private static Authentication auth(Long id, Rol rol) {
        CustomUserDetails d = new CustomUserDetails(new Usuario(id, "Nombre", "a@a.com", "pw", rol));
        return new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities());
    }

    private static final Authentication ADMIN = auth(1L, Rol.Administrador);

    private LaborRequest request(String nombre, String descripcion) {
        LaborRequest req = new LaborRequest();
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
            List<Labor> activas = List.of(
                    new Labor(1L, "Siembra", "Labor de siembra"),
                    new Labor(2L, "Cosecha", "Labor de cosecha")
            );
            when(laborUseCase.listarActivas()).thenReturn(activas);

            ResponseEntity<List<LaborResponse>> response = actividadController.listar(ADMIN);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().size());
            assertEquals("Siembra", response.getBody().get(0).nombre);
            assertEquals("Cosecha", response.getBody().get(1).nombre);
            verify(laborUseCase, times(1)).listarActivas();
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si no hay actividades activas")
        void debeRetornar200ConListaVacia() {
            when(laborUseCase.listarActivas()).thenReturn(List.of());

            ResponseEntity<List<LaborResponse>> response = actividadController.listar(ADMIN);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEmpty());
            verify(laborUseCase, times(1)).listarActivas();
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
            when(laborUseCase.crearActividad("Siembra", "Labor de siembra", null))
                    .thenReturn(actividadBase);

            ResponseEntity<LaborResponse> response =
                    actividadController.crear(request("Siembra", "Labor de siembra"), ADMIN);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().id);
            assertEquals("Siembra", response.getBody().nombre);
            assertEquals("Labor de siembra", response.getBody().descripcion);
            assertTrue(response.getBody().activa);
            verify(laborUseCase, times(1)).crearActividad("Siembra", "Labor de siembra", null);
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el nombre ya existe")
        void debePropaglarExcepcionSiNombreDuplicado() {
            when(laborUseCase.crearActividad(any(), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Siembra ya existe"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> actividadController.crear(request("Siembra", "Desc"), ADMIN)
            );

            assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Siembra"));
            verify(laborUseCase, times(1)).crearActividad(any(), any(), any());
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
            Labor actualizada = new Labor(1L, "Cosecha", "Nueva descripción");
            when(laborUseCase.actualizarActividad(1L, "Cosecha", "Nueva descripción"))
                    .thenReturn(actualizada);

            ResponseEntity<LaborResponse> response =
                    actividadController.actualizar(1L, request("Cosecha", "Nueva descripción"));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().id);
            assertEquals("Cosecha", response.getBody().nombre);
            assertEquals("Nueva descripción", response.getBody().descripcion);
            verify(laborUseCase, times(1)).actualizarActividad(1L, "Cosecha", "Nueva descripción");
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            when(laborUseCase.actualizarActividad(eq(99L), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Labor", 99L));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> actividadController.actualizar(99L, request("Cosecha", "Desc"))
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(laborUseCase, times(1)).actualizarActividad(eq(99L), any(), any());
        }

        @Test
        @DisplayName("Debe propagar BusinessException si el nuevo nombre ya existe en otra actividad")
        void debePropaglarExcepcionSiNombreDuplicadoEnOtra() {
            when(laborUseCase.actualizarActividad(eq(1L), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Cosecha ya existe"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> actividadController.actualizar(1L, request("Cosecha", "Desc"))
            );

            assertEquals(ErrorCode.RESOURCE_ALREADY_EXISTS, ex.getErrorCode());
            verify(laborUseCase, times(1)).actualizarActividad(eq(1L), any(), any());
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
            doNothing().when(laborUseCase).desactivarActividad(1L);

            ResponseEntity<Void> response = actividadController.desactivar(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(laborUseCase, times(1)).desactivarActividad(1L);
        }

        @Test
        @DisplayName("Debe propagar ResourceNotFoundException si el id no existe")
        void debePropaglarExcepcionSiIdNoExiste() {
            doThrow(new ResourceNotFoundException("Labor", 99L))
                    .when(laborUseCase).desactivarActividad(99L);

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> actividadController.desactivar(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(laborUseCase, times(1)).desactivarActividad(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Labores por agricultor
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("labores por agricultor")
    class LaboresPorAgricultor {

        @Test
        @DisplayName("El agricultor ve las comunes y las suyas")
        void agricultorVeComunesYSuyas() {
            when(laborUseCase.listarActivasPara(7L)).thenReturn(List.of(actividadBase));

            ResponseEntity<List<LaborResponse>> r = actividadController.listar(auth(7L, Rol.Agricultor));

            assertEquals(1, r.getBody().size());
            assertTrue(r.getBody().get(0).comun);
            verify(laborUseCase, never()).listarActivas();
        }

        @Test
        @DisplayName("Lo que crea el agricultor queda a su nombre")
        void agricultorCreaASuNombre() {
            Labor propia = new Labor(9L, "Deshoje", "");
            propia.setAgricultorId(7L);
            when(laborUseCase.crearActividad("Deshoje", "", 7L)).thenReturn(propia);

            ResponseEntity<LaborResponse> r = actividadController.crear(request("Deshoje", ""), auth(7L, Rol.Agricultor));

            assertFalse(r.getBody().comun);
        }
    }
}
