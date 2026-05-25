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
@DisplayName("JornalRepositoryImpl - Pruebas Unitarias")
class JornalRepositoryImplTest {

    @Mock
    private JpaJornalRepository jpa;

    @InjectMocks
    private JornalRepositoryImpl repository;

    private Jornal jornalBase;
    private JornalEntity entityBase;

    private final LocalDate FECHA  = LocalDate.of(2025, 1, 15);
    private final LocalDate INICIO = LocalDate.of(2025, 1, 1);
    private final LocalDate FIN    = LocalDate.of(2025, 1, 31);

    @BeforeEach
    void setUp() {
        // ── dominio ────────────────────────────────────────────────────────
        Cargo cargo = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);
        Trabajador trabajador = new Trabajador(
                1L, "Juan", "Pérez", "123456789", "3001234567", "Calle 1",
                LocalDate.of(2020, 1, 1), TipoContrato.JORNAL, cargo
        );
        jornalBase = new Jornal(1L, FECHA, trabajador, 10L, "Maíz", "Sin observaciones");
        jornalBase.setValorJornal(new BigDecimal("80000"));
        jornalBase.setLiquidado(false);
        jornalBase.setActividades(List.of());

        // ── entidad JPA ───────────────────────────────────────────────────
        CargoEntity cargoEntity = new CargoEntity();
        cargoEntity.setId(1L);
        cargoEntity.setNombre("Operario");
        cargoEntity.setDescripcion("Cargo operario");
        cargoEntity.setValorJornal(new BigDecimal("80000"));
        cargoEntity.setActivo(true);

        TrabajadorEntity trabajadorEntity = new TrabajadorEntity();
        trabajadorEntity.setId(1L);
        trabajadorEntity.setNombre("Juan");
        trabajadorEntity.setApellido("Pérez");
        trabajadorEntity.setCedula("123456789");
        trabajadorEntity.setTelefono("3001234567");
        trabajadorEntity.setDireccion("Calle 1");
        trabajadorEntity.setFechaIngreso(LocalDate.of(2020, 1, 1));
        trabajadorEntity.setTipoContrato(TipoContrato.JORNAL);
        trabajadorEntity.setEstado(EstadoTrabajador.ACTIVO);
        trabajadorEntity.setCargo(cargoEntity);

        entityBase = new JornalEntity();
        entityBase.setId(1L);
        entityBase.setFecha(FECHA);
        entityBase.setTrabajador(trabajadorEntity);
        entityBase.setCultivoId(10L);
        entityBase.setNombreCultivo("Maíz");
        entityBase.setObservaciones("Sin observaciones");
        entityBase.setValorJornal(new BigDecimal("80000"));
        entityBase.setLiquidado(false);
        entityBase.setActividades(List.of());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  guardar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("guardar()")
    class Guardar {

        @Test
        @DisplayName("Debe guardar y retornar el jornal mapeado al dominio")
        void debeGuardarYRetornarJornalMapeado() {
            when(jpa.save(any(JornalEntity.class))).thenReturn(entityBase);

            Jornal resultado = repository.guardar(jornalBase);

            assertNotNull(resultado);
            assertEquals(1L,                      resultado.getId());
            assertEquals(FECHA,                   resultado.getFecha());
            assertEquals(10L,                     resultado.getCultivoId());
            assertEquals("Maíz",                  resultado.getNombreCultivo());
            assertEquals(new BigDecimal("80000"), resultado.getValorJornal());
            assertFalse(resultado.isLiquidado());
            assertEquals(1L,                      resultado.getTrabajador().getId());
            verify(jpa, times(1)).save(any(JornalEntity.class));
        }

        @Test
        @DisplayName("Debe guardar y retornar correctamente un jornal liquidado")
        void debeGuardarJornalLiquidado() {
            JornalEntity entityLiquidado = new JornalEntity();
            entityLiquidado.setId(2L);
            entityLiquidado.setFecha(FECHA);
            entityLiquidado.setTrabajador(entityBase.getTrabajador());
            entityLiquidado.setCultivoId(10L);
            entityLiquidado.setNombreCultivo("Maíz");
            entityLiquidado.setObservaciones("Liquidado");
            entityLiquidado.setValorJornal(new BigDecimal("80000"));
            entityLiquidado.setLiquidado(true);
            entityLiquidado.setActividades(List.of());

            Jornal jornalLiquidado = new Jornal(2L, FECHA, jornalBase.getTrabajador(),
                    10L, "Maíz", "Liquidado");
            jornalLiquidado.setValorJornal(new BigDecimal("80000"));
            jornalLiquidado.setLiquidado(true);
            jornalLiquidado.setActividades(List.of());

            when(jpa.save(any(JornalEntity.class))).thenReturn(entityLiquidado);

            Jornal resultado = repository.guardar(jornalLiquidado);

            assertNotNull(resultado);
            assertTrue(resultado.isLiquidado());
            assertEquals(2L, resultado.getId());
            verify(jpa, times(1)).save(any(JornalEntity.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar Optional con el jornal mapeado cuando el id existe")
        void debeRetornarOptionalConJornalSiIdExiste() {
            when(jpa.findById(1L)).thenReturn(Optional.of(entityBase));

            Optional<Jornal> resultado = repository.buscarPorId(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,     resultado.get().getId());
            assertEquals(FECHA,  resultado.get().getFecha());
            assertEquals("Maíz", resultado.get().getNombreCultivo());
            verify(jpa, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el id no existe")
        void debeRetornarOptionalVacioSiIdNoExiste() {
            when(jpa.findById(99L)).thenReturn(Optional.empty());

            Optional<Jornal> resultado = repository.buscarPorId(99L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findById(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorTrabajador
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorTrabajador()")
    class BuscarPorTrabajador {

        @Test
        @DisplayName("Debe retornar los jornales del trabajador mapeados al dominio")
        void debeRetornarJornalesDelTrabajador() {
            when(jpa.findByTrabajadorId(1L)).thenReturn(List.of(entityBase));

            List<Jornal> resultado = repository.buscarPorTrabajador(1L);

            assertEquals(1, resultado.size());
            assertEquals(1L,    resultado.get(0).getId());
            assertEquals(FECHA, resultado.get(0).getFecha());
            assertEquals(1L,    resultado.get(0).getTrabajador().getId());
            verify(jpa, times(1)).findByTrabajadorId(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si el trabajador no tiene jornales")
        void debeRetornarListaVaciaSiNoHayJornales() {
            when(jpa.findByTrabajadorId(1L)).thenReturn(List.of());

            List<Jornal> resultado = repository.buscarPorTrabajador(1L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByTrabajadorId(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorTrabajadorYPeriodo
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorTrabajadorYPeriodo()")
    class BuscarPorTrabajadorYPeriodo {

        @Test
        @DisplayName("Debe retornar jornales del trabajador dentro del periodo indicado")
        void debeRetornarJornalesDentroDelPeriodo() {
            when(jpa.findByTrabajadorIdAndFechaBetween(1L, INICIO, FIN))
                    .thenReturn(List.of(entityBase));

            List<Jornal> resultado = repository.buscarPorTrabajadorYPeriodo(1L, INICIO, FIN);

            assertEquals(1,     resultado.size());
            assertEquals(FECHA, resultado.get(0).getFecha());
            assertTrue(!resultado.get(0).getFecha().isBefore(INICIO)
                    && !resultado.get(0).getFecha().isAfter(FIN));
            verify(jpa, times(1)).findByTrabajadorIdAndFechaBetween(1L, INICIO, FIN);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay jornales en el periodo")
        void debeRetornarListaVaciaSiNoHayJornalesEnElPeriodo() {
            when(jpa.findByTrabajadorIdAndFechaBetween(1L, INICIO, FIN))
                    .thenReturn(List.of());

            List<Jornal> resultado = repository.buscarPorTrabajadorYPeriodo(1L, INICIO, FIN);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByTrabajadorIdAndFechaBetween(1L, INICIO, FIN);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorCultivo
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorCultivo()")
    class BuscarPorCultivo {

        @Test
        @DisplayName("Debe retornar los jornales del cultivo mapeados al dominio")
        void debeRetornarJornalesDelCultivo() {
            when(jpa.findByCultivoId(10L)).thenReturn(List.of(entityBase));

            List<Jornal> resultado = repository.buscarPorCultivo(10L);

            assertEquals(1,    resultado.size());
            assertEquals(10L,  resultado.get(0).getCultivoId());
            assertEquals("Maíz", resultado.get(0).getNombreCultivo());
            verify(jpa, times(1)).findByCultivoId(10L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si el cultivo no tiene jornales")
        void debeRetornarListaVaciaSiCultivoSinJornales() {
            when(jpa.findByCultivoId(10L)).thenReturn(List.of());

            List<Jornal> resultado = repository.buscarPorCultivo(10L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByCultivoId(10L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorCultivoYPeriodo
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorCultivoYPeriodo()")
    class BuscarPorCultivoYPeriodo {

        @Test
        @DisplayName("Debe retornar jornales del cultivo dentro del periodo indicado")
        void debeRetornarJornalesDentroDelPeriodo() {
            when(jpa.findByCultivoIdAndFechaBetween(10L, INICIO, FIN))
                    .thenReturn(List.of(entityBase));

            List<Jornal> resultado = repository.buscarPorCultivoYPeriodo(10L, INICIO, FIN);

            assertEquals(1,    resultado.size());
            assertEquals(10L,  resultado.get(0).getCultivoId());
            assertEquals(FECHA, resultado.get(0).getFecha());
            verify(jpa, times(1)).findByCultivoIdAndFechaBetween(10L, INICIO, FIN);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay jornales del cultivo en el periodo")
        void debeRetornarListaVaciaSiNoHayJornalesEnElPeriodo() {
            when(jpa.findByCultivoIdAndFechaBetween(10L, INICIO, FIN))
                    .thenReturn(List.of());

            List<Jornal> resultado = repository.buscarPorCultivoYPeriodo(10L, INICIO, FIN);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByCultivoIdAndFechaBetween(10L, INICIO, FIN);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarNoLiquidadosPorTrabajador
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarNoLiquidadosPorTrabajador()")
    class BuscarNoLiquidadosPorTrabajador {

        @Test
        @DisplayName("Debe retornar solo los jornales no liquidados del trabajador")
        void debeRetornarJornalesNoLiquidados() {
            when(jpa.findByTrabajadorIdAndLiquidadoFalse(1L)).thenReturn(List.of(entityBase));

            List<Jornal> resultado = repository.buscarNoLiquidadosPorTrabajador(1L);

            assertEquals(1, resultado.size());
            assertFalse(resultado.get(0).isLiquidado());
            assertEquals(1L, resultado.get(0).getTrabajador().getId());
            verify(jpa, times(1)).findByTrabajadorIdAndLiquidadoFalse(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si todos los jornales están liquidados")
        void debeRetornarListaVaciaSiTodosLiquidados() {
            when(jpa.findByTrabajadorIdAndLiquidadoFalse(1L)).thenReturn(List.of());

            List<Jornal> resultado = repository.buscarNoLiquidadosPorTrabajador(1L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByTrabajadorIdAndLiquidadoFalse(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarNoLiquidadosPorTrabajadorYPeriodo
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarNoLiquidadosPorTrabajadorYPeriodo()")
    class BuscarNoLiquidadosPorTrabajadorYPeriodo {

        @Test
        @DisplayName("Debe retornar jornales no liquidados del trabajador en el periodo")
        void debeRetornarJornalesNoLiquidadosEnElPeriodo() {
            when(jpa.findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(1L, INICIO, FIN))
                    .thenReturn(List.of(entityBase));

            List<Jornal> resultado =
                    repository.buscarNoLiquidadosPorTrabajadorYPeriodo(1L, INICIO, FIN);

            assertEquals(1,     resultado.size());
            assertFalse(resultado.get(0).isLiquidado());
            assertEquals(FECHA, resultado.get(0).getFecha());
            verify(jpa, times(1))
                    .findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(1L, INICIO, FIN);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay jornales no liquidados en el periodo")
        void debeRetornarListaVaciaSiNoHayNoLiquidadosEnElPeriodo() {
            when(jpa.findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(1L, INICIO, FIN))
                    .thenReturn(List.of());

            List<Jornal> resultado =
                    repository.buscarNoLiquidadosPorTrabajadorYPeriodo(1L, INICIO, FIN);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1))
                    .findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(1L, INICIO, FIN);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarDisponiblesParaNomina
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarDisponiblesParaNomina()")
    class BuscarDisponiblesParaNomina {

        @Test
        @DisplayName("Debe retornar jornales disponibles para liquidar en la nómina del periodo")
        void debeRetornarJornalesDisponiblesParaNomina() {
            when(jpa.findDisponiblesParaNomina(1L, INICIO, FIN))
                    .thenReturn(List.of(entityBase));

            List<Jornal> resultado =
                    repository.buscarDisponiblesParaNomina(1L, INICIO, FIN);

            assertEquals(1,     resultado.size());
            assertFalse(resultado.get(0).isLiquidado());
            assertEquals(FECHA, resultado.get(0).getFecha());
            assertEquals(1L,    resultado.get(0).getTrabajador().getId());
            verify(jpa, times(1)).findDisponiblesParaNomina(1L, INICIO, FIN);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay jornales disponibles para la nómina")
        void debeRetornarListaVaciaSiNoHayDisponibles() {
            when(jpa.findDisponiblesParaNomina(1L, INICIO, FIN))
                    .thenReturn(List.of());

            List<Jornal> resultado =
                    repository.buscarDisponiblesParaNomina(1L, INICIO, FIN);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findDisponiblesParaNomina(1L, INICIO, FIN);
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