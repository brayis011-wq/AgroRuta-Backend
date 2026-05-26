package com.agroruta.agriculturalInput.infrastructure.persistence;

import com.agroruta.agriculturalInput.domain.AgriculturalInput;
import com.agroruta.agriculturalInput.domain.AgriculturalInputType;
import com.agroruta.agriculturalInput.domain.MeasurementUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgriculturalInputRepositoryAdapter - Pruebas Unitarias")
class AgriculturalInputRepositoryAdapterTest {

    @Mock
    private AgriculturalInputJpaRepository jpaRepository;

    @InjectMocks
    private AgriculturalInputRepositoryAdapter adapter;

    private AgriculturalInputEntity entityBase;
    private AgriculturalInput       domainBase;
    private LocalDateTime           fechaCreacion;

    @BeforeEach
    void setUp() {
        fechaCreacion = LocalDateTime.of(2024, 3, 10, 9, 0);

        entityBase = buildEntity(1L, "Glifosato", AgriculturalInputType.HERBICIDA,
                MeasurementUnit.LITROS, 2.5, 48, true);

        domainBase = AgriculturalInput.create(
                "Glifosato", AgriculturalInputType.HERBICIDA, MeasurementUnit.LITROS, 2.5, 48
        );
        domainBase.setId(1L);
        domainBase.setCreadoEn(fechaCreacion);
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private AgriculturalInputEntity buildEntity(Long id, String nombre,
                                                AgriculturalInputType tipo,
                                                MeasurementUnit unidad,
                                                Double dosis, Integer reentrada,
                                                boolean activo) {
        AgriculturalInputEntity e = new AgriculturalInputEntity();
        e.setId(id);
        e.setNombre(nombre);
        e.setTipo(tipo);
        e.setUnidadSugerida(unidad);
        e.setDosisSugerida(dosis);
        e.setReentradaHoras(reentrada);
        e.setActivo(activo);
        e.setCreadoEn(fechaCreacion);
        return e;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  findAllActive
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("findAllActive()")
    class FindAllActive {

        @Test
        @DisplayName("Debe retornar todos los insumos activos mapeados al dominio")
        void debeRetornarTodosLosInsumosActivosMapeados() {
            AgriculturalInputEntity otraEntity = buildEntity(
                    2L, "Clorpirifos", AgriculturalInputType.INSECTICIDA,
                    MeasurementUnit.ML, 1.0, 24, true
            );
            when(jpaRepository.findByActivoTrue()).thenReturn(List.of(entityBase, otraEntity));

            List<AgriculturalInput> resultado = adapter.findAllActive();

            assertEquals(2,             resultado.size());
            assertEquals("Glifosato",   resultado.get(0).getNombre());
            assertEquals("Clorpirifos", resultado.get(1).getNombre());
            assertTrue(resultado.get(0).isActivo());
            assertTrue(resultado.get(1).isActivo());
            verify(jpaRepository, times(1)).findByActivoTrue();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay insumos activos")
        void debeRetornarListaVaciaSiNoHayActivos() {
            when(jpaRepository.findByActivoTrue()).thenReturn(List.of());

            List<AgriculturalInput> resultado = adapter.findAllActive();

            assertTrue(resultado.isEmpty());
            verify(jpaRepository, times(1)).findByActivoTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  searchByQuery
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("searchByQuery()")
    class SearchByQuery {

        @Test
        @DisplayName("Debe retornar los insumos que coinciden con el query mapeados al dominio")
        void debeRetornarInsumosQueCoinciden() {
            when(jpaRepository.searchActiveByQuery("Glif")).thenReturn(List.of(entityBase));

            List<AgriculturalInput> resultado = adapter.searchByQuery("Glif");

            assertEquals(1,           resultado.size());
            assertEquals("Glifosato", resultado.get(0).getNombre());
            assertEquals(AgriculturalInputType.HERBICIDA, resultado.get(0).getTipo());
            verify(jpaRepository, times(1)).searchActiveByQuery("Glif");
        }

        @Test
        @DisplayName("Debe retornar lista vacía si ningún insumo coincide con el query")
        void debeRetornarListaVaciaSiNadaCoincide() {
            when(jpaRepository.searchActiveByQuery("xyz")).thenReturn(List.of());

            List<AgriculturalInput> resultado = adapter.searchByQuery("xyz");

            assertTrue(resultado.isEmpty());
            verify(jpaRepository, times(1)).searchActiveByQuery("xyz");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  findById
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Debe retornar Optional con el insumo mapeado cuando el id existe y está activo")
        void debeRetornarOptionalConInsumoSiIdExisteYActivo() {
            when(jpaRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(entityBase));

            Optional<AgriculturalInput> resultado = adapter.findById(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,                             resultado.get().getId());
            assertEquals("Glifosato",                    resultado.get().getNombre());
            assertEquals(AgriculturalInputType.HERBICIDA, resultado.get().getTipo());
            assertEquals(MeasurementUnit.LITROS,          resultado.get().getUnidadSugerida());
            assertEquals(2.5,                             resultado.get().getDosisSugerida());
            assertEquals(48,                              resultado.get().getReentradaHoras());
            assertTrue(resultado.get().isActivo());
            verify(jpaRepository, times(1)).findByIdAndActivoTrue(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el id no existe o está inactivo")
        void debeRetornarOptionalVacioSiIdNoExisteOInactivo() {
            when(jpaRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

            Optional<AgriculturalInput> resultado = adapter.findById(99L);

            assertTrue(resultado.isEmpty());
            verify(jpaRepository, times(1)).findByIdAndActivoTrue(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  save
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("Debe guardar y retornar el insumo activo mapeado al dominio")
        void debeGuardarYRetornarInsumoActivo() {
            when(jpaRepository.save(any(AgriculturalInputEntity.class))).thenReturn(entityBase);

            AgriculturalInput resultado = adapter.save(domainBase);

            assertNotNull(resultado);
            assertEquals(1L,                             resultado.getId());
            assertEquals("Glifosato",                    resultado.getNombre());
            assertEquals(AgriculturalInputType.HERBICIDA, resultado.getTipo());
            assertEquals(MeasurementUnit.LITROS,          resultado.getUnidadSugerida());
            assertEquals(2.5,                             resultado.getDosisSugerida());
            assertEquals(48,                              resultado.getReentradaHoras());
            assertTrue(resultado.isActivo());
            verify(jpaRepository, times(1)).save(any(AgriculturalInputEntity.class));
        }

        @Test
        @DisplayName("Debe guardar y retornar correctamente un insumo desactivado")
        void debeGuardarInsumoDesactivado() {
            AgriculturalInputEntity entityInactiva = buildEntity(
                    2L, "Clorpirifos", AgriculturalInputType.INSECTICIDA,
                    MeasurementUnit.ML, 1.0, 24, false
            );
            AgriculturalInput domainInactivo = AgriculturalInput.create(
                    "Clorpirifos", AgriculturalInputType.INSECTICIDA, MeasurementUnit.ML, 1.0, 24
            );
            domainInactivo.setId(2L);
            domainInactivo.deactivate();

            when(jpaRepository.save(any(AgriculturalInputEntity.class))).thenReturn(entityInactiva);

            AgriculturalInput resultado = adapter.save(domainInactivo);

            assertNotNull(resultado);
            assertEquals("Clorpirifos", resultado.getNombre());
            assertFalse(resultado.isActivo());
            verify(jpaRepository, times(1)).save(any(AgriculturalInputEntity.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  existsById
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("existsById()")
    class ExistsById {

        @Test
        @DisplayName("Debe retornar true cuando el insumo existe")
        void debeRetornarTrueSiExiste() {
            when(jpaRepository.existsById(1L)).thenReturn(true);

            boolean resultado = adapter.existsById(1L);

            assertTrue(resultado);
            verify(jpaRepository, times(1)).existsById(1L);
        }

        @Test
        @DisplayName("Debe retornar false cuando el insumo no existe")
        void debeRetornarFalseSiNoExiste() {
            when(jpaRepository.existsById(99L)).thenReturn(false);

            boolean resultado = adapter.existsById(99L);

            assertFalse(resultado);
            verify(jpaRepository, times(1)).existsById(99L);
        }
    }
}