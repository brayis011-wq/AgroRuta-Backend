package com.agroruta.agriculturalInput.application;

import com.agroruta.agriculturalInput.application.ports.in.CreateAgriculturalInputUseCase.CreateAgriculturalInputCommand;
import com.agroruta.agriculturalInput.application.ports.in.UpdateAgriculturalInputUseCase.UpdateAgriculturalInputCommand;
import com.agroruta.agriculturalInput.application.ports.out.AgriculturalInputRepositoryPort;
import com.agroruta.agriculturalInput.domain.AgriculturalInput;
import com.agroruta.agriculturalInput.domain.AgriculturalInputType;
import com.agroruta.agriculturalInput.domain.MeasurementUnit;
import com.agroruta.agriculturalInput.domain.exception.AgriculturalInputNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgriculturalInputService - Pruebas Unitarias")
class AgriculturalInputServiceTest {

    @Mock
    private AgriculturalInputRepositoryPort repository;

    @InjectMocks
    private AgriculturalInputService service;

    private AgriculturalInput inputBase;

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
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getAll
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("Debe retornar todos los insumos activos del repositorio")
        void debeRetornarTodosLosInsumosActivos() {
            AgriculturalInput otroInput = AgriculturalInput.create(
                    "Clorpirifos", AgriculturalInputType.INSECTICIDA, MeasurementUnit.ML, 1.0, 24
            );
            otroInput.setId(2L);

            when(repository.findAllActive()).thenReturn(List.of(inputBase, otroInput));

            List<AgriculturalInput> resultado = service.getAll();

            assertEquals(2,           resultado.size());
            assertEquals("Glifosato", resultado.get(0).getNombre());
            assertEquals("Clorpirifos", resultado.get(1).getNombre());
            verify(repository, times(1)).findAllActive();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay insumos activos")
        void debeRetornarListaVaciaSiNoHayInsumos() {
            when(repository.findAllActive()).thenReturn(List.of());

            List<AgriculturalInput> resultado = service.getAll();

            assertTrue(resultado.isEmpty());
            verify(repository, times(1)).findAllActive();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  search
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("Debe delegar a searchByQuery cuando el query tiene contenido")
        void debeDelegarASearchByQueryConQueryValido() {
            when(repository.searchByQuery("Glif")).thenReturn(List.of(inputBase));

            List<AgriculturalInput> resultado = service.search("Glif");

            assertEquals(1,           resultado.size());
            assertEquals("Glifosato", resultado.get(0).getNombre());
            verify(repository, times(1)).searchByQuery("Glif");
            verify(repository, never()).findAllActive();
        }

        @Test
        @DisplayName("Debe recortar espacios del query antes de buscar")
        void debeRecortarEspaciosDelQuery() {
            when(repository.searchByQuery("Glif")).thenReturn(List.of(inputBase));

            service.search("  Glif  ");

            verify(repository, times(1)).searchByQuery("Glif");
        }

        @Test
        @DisplayName("Debe retornar todos los activos cuando el query es null")
        void debeRetornarTodosActivosSiQueryEsNull() {
            when(repository.findAllActive()).thenReturn(List.of(inputBase));

            List<AgriculturalInput> resultado = service.search(null);

            assertEquals(1, resultado.size());
            verify(repository, times(1)).findAllActive();
            verify(repository, never()).searchByQuery(any());
        }

        @Test
        @DisplayName("Debe retornar todos los activos cuando el query está en blanco")
        void debeRetornarTodosActivosSiQueryEsBlanco() {
            when(repository.findAllActive()).thenReturn(List.of(inputBase));

            List<AgriculturalInput> resultado = service.search("   ");

            assertEquals(1, resultado.size());
            verify(repository, times(1)).findAllActive();
            verify(repository, never()).searchByQuery(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getById
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Debe retornar el insumo cuando el id existe")
        void debeRetornarElInsumoSiIdExiste() {
            when(repository.findById(1L)).thenReturn(Optional.of(inputBase));

            AgriculturalInput resultado = service.getById(1L);

            assertNotNull(resultado);
            assertEquals(1L,          resultado.getId());
            assertEquals("Glifosato", resultado.getNombre());
            assertTrue(resultado.isActivo());
            verify(repository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Debe lanzar AgriculturalInputNotFoundException cuando el id no existe")
        void debeLanzarExceptionSiIdNoExiste() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            AgriculturalInputNotFoundException ex = assertThrows(
                    AgriculturalInputNotFoundException.class,
                    () -> service.getById(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(repository, times(1)).findById(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  create
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Debe crear el insumo con los datos del command y guardarlo")
        void debeCrearYGuardarElInsumo() {
            CreateAgriculturalInputCommand command = new CreateAgriculturalInputCommand(
                    "Glifosato", "HERBICIDA", "LITROS", 2.5, 48
            );
            when(repository.save(any(AgriculturalInput.class))).thenReturn(inputBase);

            AgriculturalInput resultado = service.create(command);

            assertNotNull(resultado);
            assertEquals("Glifosato",                    resultado.getNombre());
            assertEquals(AgriculturalInputType.HERBICIDA, resultado.getTipo());
            assertEquals(MeasurementUnit.LITROS,          resultado.getUnidadSugerida());
            assertEquals(2.5,                             resultado.getDosisSugerida());
            assertEquals(48,                              resultado.getReentradaHoras());
            assertTrue(resultado.isActivo());
            verify(repository, times(1)).save(any(AgriculturalInput.class));
        }

        @Test
        @DisplayName("Debe crear el insumo como activo por defecto")
        void debeCrearElInsumoComoActivo() {
            CreateAgriculturalInputCommand command = new CreateAgriculturalInputCommand(
                    "Clorpirifos", "INSECTICIDA", "ML", 1.0, 24
            );
            AgriculturalInput inputCreado = AgriculturalInput.create(
                    "Clorpirifos", AgriculturalInputType.INSECTICIDA, MeasurementUnit.ML, 1.0, 24
            );
            when(repository.save(any(AgriculturalInput.class))).thenReturn(inputCreado);

            AgriculturalInput resultado = service.create(command);

            assertTrue(resultado.isActivo());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  update
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Debe actualizar el insumo existente y guardarlo")
        void debeActualizarYGuardarElInsumo() {
            UpdateAgriculturalInputCommand command = new UpdateAgriculturalInputCommand(
                    "Glifosato Pro", "HERBICIDA", "LITROS", 3.0, 72
            );
            AgriculturalInput inputActualizado = AgriculturalInput.create(
                    "Glifosato Pro", AgriculturalInputType.HERBICIDA, MeasurementUnit.LITROS, 3.0, 72
            );
            inputActualizado.setId(1L);

            when(repository.findById(1L)).thenReturn(Optional.of(inputBase));
            when(repository.save(any(AgriculturalInput.class))).thenReturn(inputActualizado);

            AgriculturalInput resultado = service.update(1L, command);

            assertNotNull(resultado);
            assertEquals("Glifosato Pro", resultado.getNombre());
            assertEquals(3.0,             resultado.getDosisSugerida());
            assertEquals(72,              resultado.getReentradaHoras());
            verify(repository, times(1)).findById(1L);
            verify(repository, times(1)).save(any(AgriculturalInput.class));
        }

        @Test
        @DisplayName("Debe lanzar AgriculturalInputNotFoundException cuando el id no existe")
        void debeLanzarExceptionSiIdNoExiste() {
            UpdateAgriculturalInputCommand command = new UpdateAgriculturalInputCommand(
                    "X", "HERBICIDA", "LITROS", 1.0, 0
            );
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    AgriculturalInputNotFoundException.class,
                    () -> service.update(99L, command)
            );

            verify(repository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  delete
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Debe desactivar el insumo y guardarlo cuando el id existe")
        void debeDesactivarYGuardarElInsumo() {
            when(repository.findById(1L)).thenReturn(Optional.of(inputBase));
            when(repository.save(any(AgriculturalInput.class))).thenReturn(inputBase);

            service.delete(1L);

            assertFalse(inputBase.isActivo());
            verify(repository, times(1)).findById(1L);
            verify(repository, times(1)).save(inputBase);
        }

        @Test
        @DisplayName("Debe lanzar AgriculturalInputNotFoundException cuando el id no existe")
        void debeLanzarExceptionSiIdNoExiste() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            AgriculturalInputNotFoundException ex = assertThrows(
                    AgriculturalInputNotFoundException.class,
                    () -> service.delete(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(repository, never()).save(any());
        }
    }
}