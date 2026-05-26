package com.agroruta.agriculturalInput.infrastructure.web;

import com.agroruta.agriculturalInput.application.ports.in.*;
import com.agroruta.agriculturalInput.domain.AgriculturalInput;
import com.agroruta.agriculturalInput.domain.AgriculturalInputType;
import com.agroruta.agriculturalInput.domain.MeasurementUnit;
import com.agroruta.agriculturalInput.infrastructure.web.dto.AgriculturalInputRequest;
import com.agroruta.agriculturalInput.infrastructure.web.dto.AgriculturalInputResponse;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgriculturalInputController - Pruebas Unitarias")
class AgriculturalInputControllerTest {

    @Mock private GetAllAgriculturalInputsUseCase  getAllUseCase;
    @Mock private SearchAgriculturalInputsUseCase  searchUseCase;
    @Mock private GetAgriculturalInputByIdUseCase  getByIdUseCase;
    @Mock private CreateAgriculturalInputUseCase   createUseCase;
    @Mock private UpdateAgriculturalInputUseCase   updateUseCase;
    @Mock private DeleteAgriculturalInputUseCase   deleteUseCase;

    @InjectMocks
    private AgriculturalInputController controller;

    private AgriculturalInput inputBase;
    private AgriculturalInputRequest requestBase;

    @BeforeEach
    void setUp() {
        inputBase = AgriculturalInput.create(
                "Glifosato",
                AgriculturalInputType.HERBICIDA,
                MeasurementUnit.LITROS,
                2.5,
                48
        );
        inputBase.setId(1L);

        requestBase = new AgriculturalInputRequest();
        requestBase.setNombre("Glifosato");
        requestBase.setTipo("HERBICIDA");
        requestBase.setUnidadSugerida("LITROS");
        requestBase.setDosisSugerida(2.5);
        requestBase.setReentradaHoras(48);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/agricultural-inputs
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("Debe retornar 200 con todos los insumos cuando q es null")
        void debeRetornarTodosLosInsumosConQNull() {
            when(getAllUseCase.getAll()).thenReturn(List.of(inputBase));

            ResponseEntity<List<AgriculturalInputResponse>> respuesta = controller.getAll(null);

            assertEquals(HttpStatus.OK,  respuesta.getStatusCode());
            assertEquals(1,              respuesta.getBody().size());
            assertEquals("Glifosato",    respuesta.getBody().get(0).getNombre());
            assertEquals("HERBICIDA",    respuesta.getBody().get(0).getTipo());
            assertEquals("Herbicida",    respuesta.getBody().get(0).getTipoDisplay());
            verify(getAllUseCase, times(1)).getAll();
            verify(searchUseCase, never()).search(any());
        }

        @Test
        @DisplayName("Debe retornar 200 con todos los insumos cuando q está en blanco")
        void debeRetornarTodosLosInsumosConQBlanco() {
            when(getAllUseCase.getAll()).thenReturn(List.of(inputBase));

            ResponseEntity<List<AgriculturalInputResponse>> respuesta = controller.getAll("   ");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            verify(getAllUseCase, times(1)).getAll();
            verify(searchUseCase, never()).search(any());
        }

        @Test
        @DisplayName("Debe delegar a searchUseCase y retornar 200 cuando q tiene contenido")
        void debeBuscarInsumosConQValido() {
            when(searchUseCase.search("Glif")).thenReturn(List.of(inputBase));

            ResponseEntity<List<AgriculturalInputResponse>> respuesta = controller.getAll("Glif");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1,             respuesta.getBody().size());
            assertEquals("Glifosato",   respuesta.getBody().get(0).getNombre());
            verify(searchUseCase, times(1)).search("Glif");
            verify(getAllUseCase, never()).getAll();
        }

        @Test
        @DisplayName("Debe retornar 200 con lista vacía si no hay insumos")
        void debeRetornarListaVaciaSiNoHayInsumos() {
            when(getAllUseCase.getAll()).thenReturn(List.of());

            ResponseEntity<List<AgriculturalInputResponse>> respuesta = controller.getAll(null);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertTrue(respuesta.getBody().isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/agricultural-inputs/{id}
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Debe retornar 200 con el DTO del insumo cuando el id existe")
        void debeRetornar200ConElInsumo() {
            when(getByIdUseCase.getById(1L)).thenReturn(inputBase);

            ResponseEntity<AgriculturalInputResponse> respuesta = controller.getById(1L);

            assertNotNull(respuesta);
            assertEquals(HttpStatus.OK,   respuesta.getStatusCode());
            assertEquals(1L,              respuesta.getBody().getId());
            assertEquals("Glifosato",     respuesta.getBody().getNombre());
            assertEquals("HERBICIDA",     respuesta.getBody().getTipo());
            assertEquals("Herbicida",     respuesta.getBody().getTipoDisplay());
            assertEquals("LITROS",        respuesta.getBody().getUnidadSugerida());
            assertEquals(2.5,             respuesta.getBody().getDosisSugerida());
            assertEquals(48,              respuesta.getBody().getReentradaHoras());
            assertTrue(respuesta.getBody().getActivo());
            verify(getByIdUseCase, times(1)).getById(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST /api/agricultural-inputs
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Debe retornar 201 CREATED con el DTO del insumo creado")
        void debeRetornar201ConElInsumoCreado() {
            when(createUseCase.create(any())).thenReturn(inputBase);

            ResponseEntity<AgriculturalInputResponse> respuesta = controller.create(requestBase);

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            assertNotNull(respuesta.getBody());
            assertEquals("Glifosato",        respuesta.getBody().getNombre());
            assertEquals("HERBICIDA",        respuesta.getBody().getTipo());
            assertEquals(2.5,                respuesta.getBody().getDosisSugerida());
            assertTrue(respuesta.getBody().getActivo());
            verify(createUseCase, times(1)).create(any());
        }

        @Test
        @DisplayName("Debe mapear el request al command antes de delegar al useCase")
        void debeMappearElRequestAlCommand() {
            when(createUseCase.create(any())).thenReturn(inputBase);

            controller.create(requestBase);

            verify(createUseCase, times(1)).create(
                    argThat(cmd ->
                            cmd.nombre().equals("Glifosato") &&
                                    cmd.tipo().equals("HERBICIDA")   &&
                                    cmd.unidadSugerida().equals("LITROS")
                    )
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUT /api/agricultural-inputs/{id}
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Debe retornar 200 con el DTO del insumo actualizado")
        void debeRetornar200ConElInsumoActualizado() {
            when(updateUseCase.update(eq(1L), any())).thenReturn(inputBase);

            ResponseEntity<AgriculturalInputResponse> respuesta = controller.update(1L, requestBase);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertNotNull(respuesta.getBody());
            assertEquals("Glifosato",   respuesta.getBody().getNombre());
            verify(updateUseCase, times(1)).update(eq(1L), any());
        }

        @Test
        @DisplayName("Debe mapear el request al command antes de delegar al useCase")
        void debeMappearElRequestAlCommand() {
            when(updateUseCase.update(eq(1L), any())).thenReturn(inputBase);

            controller.update(1L, requestBase);

            verify(updateUseCase, times(1)).update(
                    eq(1L),
                    argThat(cmd ->
                            cmd.nombre().equals("Glifosato") &&
                                    cmd.tipo().equals("HERBICIDA")   &&
                                    cmd.dosisSugerida().equals(2.5)
                    )
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE /api/agricultural-inputs/{id}
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Debe retornar 204 NO_CONTENT cuando el insumo es eliminado")
        void debeRetornar204AlEliminar() {
            doNothing().when(deleteUseCase).delete(1L);

            ResponseEntity<Void> respuesta = controller.delete(1L);

            assertEquals(HttpStatus.NO_CONTENT, respuesta.getStatusCode());
            assertNull(respuesta.getBody());
            verify(deleteUseCase, times(1)).delete(1L);
        }
    }
}