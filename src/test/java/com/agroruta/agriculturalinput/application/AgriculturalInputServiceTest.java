package com.agroruta.agriculturalinput.application;

import com.agroruta.agriculturalinput.application.ports.in.CreateAgriculturalInputUseCase.CreateAgriculturalInputCommand;
import com.agroruta.agriculturalinput.application.ports.in.UpdateAgriculturalInputUseCase.UpdateAgriculturalInputCommand;
import com.agroruta.agriculturalinput.application.ports.out.AgriculturalInputRepositoryPort;
import com.agroruta.agriculturalinput.domain.AgriculturalInput;
import com.agroruta.agriculturalinput.domain.AgriculturalInputType;
import com.agroruta.agriculturalinput.domain.MeasurementUnit;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.exception.BusinessException;
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
        @DisplayName("Debe lanzar ResourceNotFoundException cuando el id no existe")
        void debeLanzarExceptionSiIdNoExiste() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
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
        @DisplayName("Debe lanzar ResourceNotFoundException cuando el id no existe")
        void debeLanzarExceptionSiIdNoExiste() {
            UpdateAgriculturalInputCommand command = new UpdateAgriculturalInputCommand(
                    "X", "HERBICIDA", "LITROS", 1.0, 0
            );
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
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
        @DisplayName("Debe lanzar ResourceNotFoundException cuando el id no existe")
        void debeLanzarExceptionSiIdNoExiste() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.delete(99L)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(repository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Nombres repetidos y estado (vista del Administrador)
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("nombres y estado")
    class NombresYEstado {

        @Test
        @DisplayName("create() quita espacios de más en el nombre")
        void createLimpiaElNombre() {
            when(repository.save(any(AgriculturalInput.class))).thenAnswer(inv -> inv.getArgument(0));

            AgriculturalInput resultado = service.create(new CreateAgriculturalInputCommand(
                    "  Mancozeb   80 WP ", "FUNGICIDA", "GRAMOS", 2.5, 24));

            assertEquals("Mancozeb 80 WP", resultado.getNombre());
            verify(repository).existsActiveByNombre("Mancozeb 80 WP", null);
        }

        @Test
        @DisplayName("create() rechaza un nombre que ya usa otro insumo activo")
        void createRechazaNombreRepetido() {
            when(repository.existsActiveByNombre("Glifosato", null)).thenReturn(true);

            assertThrows(BusinessException.class, () -> service.create(
                    new CreateAgriculturalInputCommand("Glifosato", "HERBICIDA", "LITROS", 2.5, 48)));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("update() permite dejar el mismo nombre y rechaza el de otro insumo")
        void updateExcluyeElMismoInsumo() {
            when(repository.findById(1L)).thenReturn(Optional.of(inputBase));
            when(repository.existsActiveByNombre("Clorpirifos", 1L)).thenReturn(true);

            assertThrows(BusinessException.class, () -> service.update(1L,
                    new UpdateAgriculturalInputCommand("Clorpirifos", "HERBICIDA", "LITROS", 2.5, 48)));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("create() rechaza un nombre vacío")
        void createRechazaNombreVacio() {
            assertThrows(IllegalArgumentException.class, () -> service.create(
                    new CreateAgriculturalInputCommand("   ", "HERBICIDA", "LITROS", 2.5, 48)));
        }

        @Test
        @DisplayName("getAllIncludingInactive() devuelve también los inactivos")
        void listaTambienInactivos() {
            inputBase.deactivate();
            when(repository.findAll()).thenReturn(List.of(inputBase));

            assertEquals(1, service.getAllIncludingInactive().size());
            verify(repository, never()).findAllActive();
        }

        @Test
        @DisplayName("changeStatus() vuelve a activar un insumo desactivado")
        void reactivaUnInsumo() {
            inputBase.deactivate();
            when(repository.findAnyById(1L)).thenReturn(Optional.of(inputBase));
            when(repository.save(inputBase)).thenReturn(inputBase);

            AgriculturalInput resultado = service.changeStatus(1L, true);

            assertTrue(resultado.isActivo());
        }

        @Test
        @DisplayName("changeStatus() no reactiva si ya hay otro activo con el mismo nombre")
        void noReactivaSiElNombreEstaOcupado() {
            inputBase.deactivate();
            when(repository.findAnyById(1L)).thenReturn(Optional.of(inputBase));
            when(repository.existsActiveByNombre("Glifosato", 1L)).thenReturn(true);

            assertThrows(BusinessException.class, () -> service.changeStatus(1L, true));
            assertFalse(inputBase.isActivo());
        }

        @Test
        @DisplayName("changeStatus() sin cambio no guarda nada")
        void sinCambioNoGuarda() {
            when(repository.findAnyById(1L)).thenReturn(Optional.of(inputBase));

            service.changeStatus(1L, true);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("changeStatus() con id inexistente lanza ResourceNotFoundException")
        void estadoConIdInexistente() {
            when(repository.findAnyById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.changeStatus(99L, false));
        }
    }
}
