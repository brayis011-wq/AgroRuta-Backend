package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.Cargo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CargoRepositoryImpl - Pruebas Unitarias")
class CargoRepositoryImplTest {

    @Mock
    private JpaCargoRepository jpa;

    @InjectMocks
    private CargoRepositoryImpl repository;

    private Cargo cargoBase;
    private CargoEntity entityBase;

    @BeforeEach
    void setUp() {
        cargoBase = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);

        entityBase = new CargoEntity();
        entityBase.setId(1L);
        entityBase.setNombre("Operario");
        entityBase.setDescripcion("Cargo operario");
        entityBase.setValorJornal(new BigDecimal("80000"));
        entityBase.setActivo(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  guardar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("guardar()")
    class Guardar {

        @Test
        @DisplayName("Debe guardar y retornar el cargo mapeado al dominio")
        void debeGuardarYRetornarCargoMapeado() {
            when(jpa.save(any(CargoEntity.class))).thenReturn(entityBase);

            Cargo resultado = repository.guardar(cargoBase);

            assertNotNull(resultado);
            assertEquals(1L,                       resultado.getId());
            assertEquals("Operario",               resultado.getNombre());
            assertEquals("Cargo operario",         resultado.getDescripcion());
            assertEquals(new BigDecimal("80000"),  resultado.getValorJornal());
            assertTrue(resultado.isActivo());
            verify(jpa, times(1)).save(any(CargoEntity.class));
        }

        @Test
        @DisplayName("Debe guardar y retornar correctamente un cargo inactivo")
        void debeGuardarCargoInactivo() {
            CargoEntity entityInactivo = new CargoEntity();
            entityInactivo.setId(2L);
            entityInactivo.setNombre("Supervisor");
            entityInactivo.setDescripcion("Cargo supervisor");
            entityInactivo.setValorJornal(new BigDecimal("120000"));
            entityInactivo.setActivo(false);

            Cargo cargoInactivo = new Cargo(2L, "Supervisor", "Cargo supervisor",
                    new BigDecimal("120000"), false);

            when(jpa.save(any(CargoEntity.class))).thenReturn(entityInactivo);

            Cargo resultado = repository.guardar(cargoInactivo);

            assertNotNull(resultado);
            assertEquals("Supervisor", resultado.getNombre());
            assertFalse(resultado.isActivo());
            verify(jpa, times(1)).save(any(CargoEntity.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar Optional con el cargo mapeado cuando el id existe")
        void debeRetornarOptionalConCargoSiIdExiste() {
            when(jpa.findById(1L)).thenReturn(Optional.of(entityBase));

            Optional<Cargo> resultado = repository.buscarPorId(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,                      resultado.get().getId());
            assertEquals("Operario",              resultado.get().getNombre());
            assertEquals(new BigDecimal("80000"), resultado.get().getValorJornal());
            assertTrue(resultado.get().isActivo());
            verify(jpa, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el id no existe")
        void debeRetornarOptionalVacioSiIdNoExiste() {
            when(jpa.findById(99L)).thenReturn(Optional.empty());

            Optional<Cargo> resultado = repository.buscarPorId(99L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findById(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  listarTodos
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Debe retornar todos los cargos mapeados al dominio")
        void debeRetornarTodosLosCargos() {
            CargoEntity otraEntity = new CargoEntity();
            otraEntity.setId(2L);
            otraEntity.setNombre("Supervisor");
            otraEntity.setDescripcion("Cargo supervisor");
            otraEntity.setValorJornal(new BigDecimal("120000"));
            otraEntity.setActivo(false);

            when(jpa.findAll()).thenReturn(List.of(entityBase, otraEntity));

            List<Cargo> resultado = repository.listarTodos();

            assertEquals(2,            resultado.size());
            assertEquals("Operario",   resultado.get(0).getNombre());
            assertEquals("Supervisor", resultado.get(1).getNombre());
            assertTrue(resultado.get(0).isActivo());
            assertFalse(resultado.get(1).isActivo());
            verify(jpa, times(1)).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay cargos registrados")
        void debeRetornarListaVaciaSiNoHayCargos() {
            when(jpa.findAll()).thenReturn(List.of());

            List<Cargo> resultado = repository.listarTodos();

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findAll();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  listarActivos
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarActivos()")
    class ListarActivos {

        @Test
        @DisplayName("Debe retornar solo los cargos con activo=true mapeados al dominio")
        void debeRetornarSoloCargosActivos() {
            when(jpa.findByActivoTrue()).thenReturn(List.of(entityBase));

            List<Cargo> resultado = repository.listarActivos();

            assertEquals(1,          resultado.size());
            assertEquals("Operario", resultado.get(0).getNombre());
            assertTrue(resultado.get(0).isActivo());
            verify(jpa, times(1)).findByActivoTrue();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay cargos activos")
        void debeRetornarListaVaciaSiNoHayActivos() {
            when(jpa.findByActivoTrue()).thenReturn(List.of());

            List<Cargo> resultado = repository.listarActivos();

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByActivoTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  existsByNombre
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("existsByNombre()")
    class ExistsByNombre {

        @Test
        @DisplayName("Debe retornar true cuando el nombre ya está registrado")
        void debeRetornarTrueSiNombreExiste() {
            when(jpa.existsByNombre("Operario")).thenReturn(true);

            boolean resultado = repository.existsByNombre("Operario");

            assertTrue(resultado);
            verify(jpa, times(1)).existsByNombre("Operario");
        }

        @Test
        @DisplayName("Debe retornar false cuando el nombre no está registrado")
        void debeRetornarFalseSiNombreNoExiste() {
            when(jpa.existsByNombre("Técnico")).thenReturn(false);

            boolean resultado = repository.existsByNombre("Técnico");

            assertFalse(resultado);
            verify(jpa, times(1)).existsByNombre("Técnico");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  existsByNombreAndIdNot
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("existsByNombreAndIdNot()")
    class ExistsByNombreAndIdNot {

        @Test
        @DisplayName("Debe retornar true cuando otro registro usa el mismo nombre")
        void debeRetornarTrueSiHayConflictoDeNombre() {
            when(jpa.existsByNombreAndIdNot("Operario", 2L)).thenReturn(true);

            boolean resultado = repository.existsByNombreAndIdNot("Operario", 2L);

            assertTrue(resultado);
            verify(jpa, times(1)).existsByNombreAndIdNot("Operario", 2L);
        }

        @Test
        @DisplayName("Debe retornar false cuando el propio registro tiene ese nombre (sin conflicto)")
        void debeRetornarFalseSiElMismoRegistroTieneElNombre() {
            when(jpa.existsByNombreAndIdNot("Operario", 1L)).thenReturn(false);

            boolean resultado = repository.existsByNombreAndIdNot("Operario", 1L);

            assertFalse(resultado);
            verify(jpa, times(1)).existsByNombreAndIdNot("Operario", 1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  eliminar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        @Test
        @DisplayName("Debe delegar la eliminación al repositorio JPA con el id correcto")
        void debeDelegarEliminacionAlJpa() {
            doNothing().when(jpa).deleteById(1L);

            repository.eliminar(1L);

            verify(jpa, times(1)).deleteById(1L);
        }
    }
}