package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrabajadorRepositoryImpl - Pruebas Unitarias")
class TrabajadorRepositoryImplTest {

    @Mock
    private JpaTrabajadorRepository jpa;

    @InjectMocks
    private TrabajadorRepositoryImpl repository;

    private Trabajador trabajadorBase;
    private TrabajadorEntity entityBase;

    @BeforeEach
    void setUp() {
        // ── dominio ────────────────────────────────────────────────────────
        Cargo cargo = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);
        trabajadorBase = new Trabajador(
                1L, "Juan", "Pérez", "123456789", "3001234567", "Calle 1",
                LocalDate.of(2020, 1, 1), TipoContrato.JORNAL, cargo
        );
        trabajadorBase.setEstado(EstadoTrabajador.ACTIVO);

        // ── entidad JPA ───────────────────────────────────────────────────
        CargoEntity cargoEntity = new CargoEntity();
        cargoEntity.setId(1L);
        cargoEntity.setNombre("Operario");
        cargoEntity.setDescripcion("Cargo operario");
        cargoEntity.setValorJornal(new BigDecimal("80000"));
        cargoEntity.setActivo(true);

        entityBase = new TrabajadorEntity();
        entityBase.setId(1L);
        entityBase.setNombre("Juan");
        entityBase.setApellido("Pérez");
        entityBase.setCedula("123456789");
        entityBase.setTelefono("3001234567");
        entityBase.setDireccion("Calle 1");
        entityBase.setFechaIngreso(LocalDate.of(2020, 1, 1));
        entityBase.setTipoContrato(TipoContrato.JORNAL);
        entityBase.setEstado(EstadoTrabajador.ACTIVO);
        entityBase.setCargo(cargoEntity);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  guardar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("guardar()")
    class Guardar {

        @Test
        @DisplayName("Debe guardar y retornar el trabajador mapeado al dominio")
        void debeGuardarYRetornarTrabajadorMapeado() {
            when(jpa.save(any(TrabajadorEntity.class))).thenReturn(entityBase);

            Trabajador resultado = repository.guardar(trabajadorBase);

            assertNotNull(resultado);
            assertEquals(1L,                      resultado.getId());
            assertEquals("Juan",                  resultado.getNombre());
            assertEquals("Pérez",                 resultado.getApellido());
            assertEquals("123456789",             resultado.getCedula());
            assertEquals(TipoContrato.JORNAL,     resultado.getTipoContrato());
            assertEquals(EstadoTrabajador.ACTIVO, resultado.getEstado());
            assertEquals(1L,                      resultado.getCargo().getId());
            verify(jpa, times(1)).save(any(TrabajadorEntity.class));
        }

        @Test
        @DisplayName("Debe guardar correctamente un trabajador en estado INACTIVO")
        void debeGuardarTrabajadorInactivo() {
            TrabajadorEntity entityInactivo = new TrabajadorEntity();
            entityInactivo.setId(2L);
            entityInactivo.setNombre("María");
            entityInactivo.setApellido("López");
            entityInactivo.setCedula("987654321");
            entityInactivo.setTelefono("3109876543");
            entityInactivo.setDireccion("Carrera 2");
            entityInactivo.setFechaIngreso(LocalDate.of(2021, 6, 1));
            entityInactivo.setTipoContrato(TipoContrato.JORNAL);
            entityInactivo.setEstado(EstadoTrabajador.INACTIVO);
            entityInactivo.setCargo(entityBase.getCargo());

            Trabajador trabajadorInactivo = new Trabajador(
                    2L, "María", "López", "987654321", "3109876543", "Carrera 2",
                    LocalDate.of(2021, 6, 1), TipoContrato.JORNAL, trabajadorBase.getCargo()
            );
            trabajadorInactivo.setEstado(EstadoTrabajador.INACTIVO);

            when(jpa.save(any(TrabajadorEntity.class))).thenReturn(entityInactivo);

            Trabajador resultado = repository.guardar(trabajadorInactivo);

            assertNotNull(resultado);
            assertEquals(2L,                        resultado.getId());
            assertEquals(EstadoTrabajador.INACTIVO, resultado.getEstado());
            assertEquals("987654321",               resultado.getCedula());
            verify(jpa, times(1)).save(any(TrabajadorEntity.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar Optional con el trabajador mapeado cuando el id existe")
        void debeRetornarOptionalConTrabajadorSiIdExiste() {
            when(jpa.findById(1L)).thenReturn(Optional.of(entityBase));

            Optional<Trabajador> resultado = repository.buscarPorId(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,                      resultado.get().getId());
            assertEquals("Juan",                  resultado.get().getNombre());
            assertEquals("123456789",             resultado.get().getCedula());
            assertEquals(EstadoTrabajador.ACTIVO, resultado.get().getEstado());
            verify(jpa, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el id no existe")
        void debeRetornarOptionalVacioSiIdNoExiste() {
            when(jpa.findById(99L)).thenReturn(Optional.empty());

            Optional<Trabajador> resultado = repository.buscarPorId(99L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findById(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorCedula
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorCedula()")
    class BuscarPorCedula {

        @Test
        @DisplayName("Debe retornar Optional con el trabajador mapeado cuando la cédula existe")
        void debeRetornarTrabajadorSiCedulaExiste() {
            when(jpa.findByCedula("123456789")).thenReturn(Optional.of(entityBase));

            Optional<Trabajador> resultado = repository.buscarPorCedula("123456789");

            assertTrue(resultado.isPresent());
            assertEquals(1L,          resultado.get().getId());
            assertEquals("123456789", resultado.get().getCedula());
            assertEquals("Juan",      resultado.get().getNombre());
            verify(jpa, times(1)).findByCedula("123456789");
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando la cédula no existe")
        void debeRetornarOptionalVacioSiCedulaNoExiste() {
            when(jpa.findByCedula("000000000")).thenReturn(Optional.empty());

            Optional<Trabajador> resultado = repository.buscarPorCedula("000000000");

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByCedula("000000000");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  listarTodos
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Debe retornar todos los trabajadores mapeados al dominio")
        void debeRetornarTodosLosTrabajadores() {
            TrabajadorEntity otraEntity = new TrabajadorEntity();
            otraEntity.setId(2L);
            otraEntity.setNombre("María");
            otraEntity.setApellido("López");
            otraEntity.setCedula("987654321");
            otraEntity.setTelefono("3109876543");
            otraEntity.setDireccion("Carrera 2");
            otraEntity.setFechaIngreso(LocalDate.of(2021, 6, 1));
            otraEntity.setTipoContrato(TipoContrato.JORNAL);
            otraEntity.setEstado(EstadoTrabajador.INACTIVO);
            otraEntity.setCargo(entityBase.getCargo());

            when(jpa.findAll()).thenReturn(List.of(entityBase, otraEntity));

            List<Trabajador> resultado = repository.listarTodos();

            assertEquals(2,                         resultado.size());
            assertEquals(1L,                        resultado.get(0).getId());
            assertEquals(2L,                        resultado.get(1).getId());
            assertEquals(EstadoTrabajador.ACTIVO,   resultado.get(0).getEstado());
            assertEquals(EstadoTrabajador.INACTIVO, resultado.get(1).getEstado());
            verify(jpa, times(1)).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay trabajadores registrados")
        void debeRetornarListaVaciaSiNoHayTrabajadores() {
            when(jpa.findAll()).thenReturn(List.of());

            List<Trabajador> resultado = repository.listarTodos();

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
        @DisplayName("Debe retornar únicamente los trabajadores con estado ACTIVO")
        void debeRetornarSoloTrabajadoresActivos() {
            when(jpa.findByEstado(EstadoTrabajador.ACTIVO)).thenReturn(List.of(entityBase));

            List<Trabajador> resultado = repository.listarActivos();

            assertEquals(1, resultado.size());
            assertEquals(EstadoTrabajador.ACTIVO, resultado.get(0).getEstado());
            assertEquals(1L,                      resultado.get(0).getId());
            verify(jpa, times(1)).findByEstado(EstadoTrabajador.ACTIVO);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay trabajadores activos")
        void debeRetornarListaVaciaSiNoHayActivos() {
            when(jpa.findByEstado(EstadoTrabajador.ACTIVO)).thenReturn(List.of());

            List<Trabajador> resultado = repository.listarActivos();

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByEstado(EstadoTrabajador.ACTIVO);
        }

        @Test
        @DisplayName("Debe delegar siempre con EstadoTrabajador.ACTIVO sin importar cuántos haya")
        void debeDelegarConEstadoActivoSiempre() {
            TrabajadorEntity segundoActivo = new TrabajadorEntity();
            segundoActivo.setId(2L);
            segundoActivo.setNombre("María");
            segundoActivo.setApellido("López");
            segundoActivo.setCedula("987654321");
            segundoActivo.setTelefono("3109876543");
            segundoActivo.setDireccion("Carrera 2");
            segundoActivo.setFechaIngreso(LocalDate.of(2021, 6, 1));
            segundoActivo.setTipoContrato(TipoContrato.JORNAL);
            segundoActivo.setEstado(EstadoTrabajador.ACTIVO);
            segundoActivo.setCargo(entityBase.getCargo());

            when(jpa.findByEstado(EstadoTrabajador.ACTIVO)).thenReturn(List.of(entityBase, segundoActivo));

            List<Trabajador> resultado = repository.listarActivos();

            assertEquals(2, resultado.size());
            resultado.forEach(t -> assertEquals(EstadoTrabajador.ACTIVO, t.getEstado()));
            verify(jpa, times(1)).findByEstado(EstadoTrabajador.ACTIVO);
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

    // ══════════════════════════════════════════════════════════════════════
    //  existePorCedula
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("existePorCedula()")
    class ExistePorCedula {

        @Test
        @DisplayName("Debe retornar true cuando la cédula ya está registrada")
        void debeRetornarTrueSiCedulaExiste() {
            when(jpa.existsByCedula("123456789")).thenReturn(true);

            boolean resultado = repository.existePorCedula("123456789");

            assertTrue(resultado);
            verify(jpa, times(1)).existsByCedula("123456789");
        }

        @Test
        @DisplayName("Debe retornar false cuando la cédula no está registrada")
        void debeRetornarFalseSiCedulaNoExiste() {
            when(jpa.existsByCedula("000000000")).thenReturn(false);

            boolean resultado = repository.existePorCedula("000000000");

            assertFalse(resultado);
            verify(jpa, times(1)).existsByCedula("000000000");
        }
    }
}