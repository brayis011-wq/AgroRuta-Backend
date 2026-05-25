package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.Actividad;
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
@DisplayName("ActividadRepositoryImpl - Pruebas Unitarias")
class ActividadRepositoryImplTest {

    @Mock
    private JpaActividadRepository jpa;

    @InjectMocks
    private ActividadRepositoryImpl repository;

    private Actividad actividadBase;
    private ActividadEntity entityBase;

    @BeforeEach
    void setUp() {
        actividadBase = new Actividad(1L, "Fumigación", "Aplicación de pesticidas");

        entityBase = new ActividadEntity();
        entityBase.setId(1L);
        entityBase.setNombre("Fumigación");
        entityBase.setDescripcion("Aplicación de pesticidas");
        entityBase.setActiva(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  guardar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("guardar()")
    class Guardar {

        @Test
        @DisplayName("Debe guardar y retornar la actividad mapeada al dominio")
        void debeGuardarYRetornarActividadMapeada() {
            when(jpa.save(any(ActividadEntity.class))).thenReturn(entityBase);

            Actividad resultado = repository.guardar(actividadBase);

            assertNotNull(resultado);
            assertEquals(1L,                         resultado.getId());
            assertEquals("Fumigación",               resultado.getNombre());
            assertEquals("Aplicación de pesticidas", resultado.getDescripcion());
            assertTrue(resultado.isActiva());
            verify(jpa, times(1)).save(any(ActividadEntity.class));
        }

        @Test
        @DisplayName("Debe guardar y retornar correctamente una actividad desactivada")
        void debeGuardarActividadDesactivada() {
            ActividadEntity entityInactiva = new ActividadEntity();
            entityInactiva.setId(2L);
            entityInactiva.setNombre("Riego");
            entityInactiva.setDescripcion("Riego por goteo");
            entityInactiva.setActiva(false);

            Actividad actividadInactiva = new Actividad(2L, "Riego", "Riego por goteo");
            actividadInactiva.desactivar();

            when(jpa.save(any(ActividadEntity.class))).thenReturn(entityInactiva);

            Actividad resultado = repository.guardar(actividadInactiva);

            assertNotNull(resultado);
            assertEquals("Riego", resultado.getNombre());
            assertFalse(resultado.isActiva());
            verify(jpa, times(1)).save(any(ActividadEntity.class));
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
            when(jpa.existsByNombre("Fumigación")).thenReturn(true);

            boolean resultado = repository.existsByNombre("Fumigación");

            assertTrue(resultado);
            verify(jpa, times(1)).existsByNombre("Fumigación");
        }

        @Test
        @DisplayName("Debe retornar false cuando el nombre no está registrado")
        void debeRetornarFalseSiNombreNoExiste() {
            when(jpa.existsByNombre("Poda")).thenReturn(false);

            boolean resultado = repository.existsByNombre("Poda");

            assertFalse(resultado);
            verify(jpa, times(1)).existsByNombre("Poda");
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
            when(jpa.existsByNombreAndIdNot("Fumigación", 2L)).thenReturn(true);

            boolean resultado = repository.existsByNombreAndIdNot("Fumigación", 2L);

            assertTrue(resultado);
            verify(jpa, times(1)).existsByNombreAndIdNot("Fumigación", 2L);
        }

        @Test
        @DisplayName("Debe retornar false cuando el propio registro tiene ese nombre (sin conflicto)")
        void debeRetornarFalseSiElMismoRegistroTieneElNombre() {
            when(jpa.existsByNombreAndIdNot("Fumigación", 1L)).thenReturn(false);

            boolean resultado = repository.existsByNombreAndIdNot("Fumigación", 1L);

            assertFalse(resultado);
            verify(jpa, times(1)).existsByNombreAndIdNot("Fumigación", 1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar Optional con la actividad mapeada cuando el id existe")
        void debeRetornarOptionalConActividadSiIdExiste() {
            when(jpa.findById(1L)).thenReturn(Optional.of(entityBase));

            Optional<Actividad> resultado = repository.buscarPorId(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,           resultado.get().getId());
            assertEquals("Fumigación", resultado.get().getNombre());
            assertTrue(resultado.get().isActiva());
            verify(jpa, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el id no existe")
        void debeRetornarOptionalVacioSiIdNoExiste() {
            when(jpa.findById(99L)).thenReturn(Optional.empty());

            Optional<Actividad> resultado = repository.buscarPorId(99L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findById(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  listarTodas
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarTodas()")
    class ListarTodas {

        @Test
        @DisplayName("Debe retornar todas las actividades mapeadas al dominio")
        void debeRetornarTodasLasActividades() {
            ActividadEntity otraEntity = new ActividadEntity();
            otraEntity.setId(2L);
            otraEntity.setNombre("Riego");
            otraEntity.setDescripcion("Riego por aspersión");
            otraEntity.setActiva(false);

            when(jpa.findAll()).thenReturn(List.of(entityBase, otraEntity));

            List<Actividad> resultado = repository.listarTodas();

            assertEquals(2,            resultado.size());
            assertEquals("Fumigación", resultado.get(0).getNombre());
            assertEquals("Riego",      resultado.get(1).getNombre());
            assertTrue(resultado.get(0).isActiva());
            assertFalse(resultado.get(1).isActiva());
            verify(jpa, times(1)).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay actividades registradas")
        void debeRetornarListaVaciaSiNoHayActividades() {
            when(jpa.findAll()).thenReturn(List.of());

            List<Actividad> resultado = repository.listarTodas();

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findAll();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  listarActivas
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarActivas()")
    class ListarActivas {

        @Test
        @DisplayName("Debe retornar solo las actividades con activa=true mapeadas al dominio")
        void debeRetornarSoloActividadesActivas() {
            when(jpa.findByActivaTrue()).thenReturn(List.of(entityBase));

            List<Actividad> resultado = repository.listarActivas();

            assertEquals(1,            resultado.size());
            assertEquals("Fumigación", resultado.get(0).getNombre());
            assertTrue(resultado.get(0).isActiva());
            verify(jpa, times(1)).findByActivaTrue();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay actividades activas")
        void debeRetornarListaVaciaSiNoHayActivas() {
            when(jpa.findByActivaTrue()).thenReturn(List.of());

            List<Actividad> resultado = repository.listarActivas();

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByActivaTrue();
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