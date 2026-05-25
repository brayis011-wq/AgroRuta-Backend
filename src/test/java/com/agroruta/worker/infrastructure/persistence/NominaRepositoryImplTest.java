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
@DisplayName("NominaRepositoryImpl - Pruebas Unitarias")
class NominaRepositoryImplTest {

    @Mock
    private JpaNominaRepository jpa;

    @InjectMocks
    private NominaRepositoryImpl repository;

    private Nomina nominaBase;
    private NominaEntity entityBase;

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
        nominaBase = new Nomina(1L, trabajador, INICIO, FIN, List.of());
        nominaBase.setEstado(EstadoNomina.PENDIENTE);
        nominaBase.setObservaciones("Nómina enero");

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

        entityBase = new NominaEntity();
        entityBase.setId(1L);
        entityBase.setTrabajador(trabajadorEntity);
        entityBase.setPeriodoInicio(INICIO);
        entityBase.setPeriodoFin(FIN);
        entityBase.setTotalJornales(0);
        entityBase.setValorTotal(BigDecimal.ZERO);
        entityBase.setEstado(EstadoNomina.PENDIENTE);
        entityBase.setObservaciones("Nómina enero");
        entityBase.setJornales(List.of());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  guardar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("guardar()")
    class Guardar {

        @Test
        @DisplayName("Debe guardar y retornar la nómina mapeada al dominio")
        void debeGuardarYRetornarNominaMapeada() {
            when(jpa.save(any(NominaEntity.class))).thenReturn(entityBase);

            Nomina resultado = repository.guardar(nominaBase);

            assertNotNull(resultado);
            assertEquals(1L,                   resultado.getId());
            assertEquals(INICIO,               resultado.getPeriodoInicio());
            assertEquals(FIN,                  resultado.getPeriodoFin());
            assertEquals(EstadoNomina.PENDIENTE, resultado.getEstado());
            assertEquals("Nómina enero",       resultado.getObservaciones());
            assertEquals(1L,                   resultado.getTrabajador().getId());
            verify(jpa, times(1)).save(any(NominaEntity.class));
        }

        @Test
        @DisplayName("Debe guardar y retornar correctamente una nómina en estado APROBADA")
        void debeGuardarNominaAprobada() {
            NominaEntity entityAprobada = new NominaEntity();
            entityAprobada.setId(2L);
            entityAprobada.setTrabajador(entityBase.getTrabajador());
            entityAprobada.setPeriodoInicio(INICIO);
            entityAprobada.setPeriodoFin(FIN);
            entityAprobada.setTotalJornales(0);
            entityAprobada.setValorTotal(BigDecimal.ZERO);
            entityAprobada.setEstado(EstadoNomina.APROBADA);
            entityAprobada.setJornales(List.of());

            Nomina nominaAprobada = new Nomina(2L, nominaBase.getTrabajador(), INICIO, FIN, List.of());
            nominaAprobada.setEstado(EstadoNomina.APROBADA);

            when(jpa.save(any(NominaEntity.class))).thenReturn(entityAprobada);

            Nomina resultado = repository.guardar(nominaAprobada);

            assertNotNull(resultado);
            assertEquals(EstadoNomina.APROBADA, resultado.getEstado());
            assertEquals(2L, resultado.getId());
            verify(jpa, times(1)).save(any(NominaEntity.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar Optional con la nómina mapeada cuando el id existe")
        void debeRetornarOptionalConNominaSiIdExiste() {
            when(jpa.findById(1L)).thenReturn(Optional.of(entityBase));

            Optional<Nomina> resultado = repository.buscarPorId(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,     resultado.get().getId());
            assertEquals(INICIO, resultado.get().getPeriodoInicio());
            assertEquals(FIN,    resultado.get().getPeriodoFin());
            assertEquals(EstadoNomina.PENDIENTE, resultado.get().getEstado());
            verify(jpa, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el id no existe")
        void debeRetornarOptionalVacioSiIdNoExiste() {
            when(jpa.findById(99L)).thenReturn(Optional.empty());

            Optional<Nomina> resultado = repository.buscarPorId(99L);

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
        @DisplayName("Debe retornar las nóminas del trabajador mapeadas al dominio")
        void debeRetornarNominasDelTrabajador() {
            when(jpa.findByTrabajadorId(1L)).thenReturn(List.of(entityBase));

            List<Nomina> resultado = repository.buscarPorTrabajador(1L);

            assertEquals(1,  resultado.size());
            assertEquals(1L, resultado.get(0).getId());
            assertEquals(1L, resultado.get(0).getTrabajador().getId());
            verify(jpa, times(1)).findByTrabajadorId(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si el trabajador no tiene nóminas")
        void debeRetornarListaVaciaSiTrabajadorSinNominas() {
            when(jpa.findByTrabajadorId(1L)).thenReturn(List.of());

            List<Nomina> resultado = repository.buscarPorTrabajador(1L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByTrabajadorId(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorTrabajadorYEstado
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorTrabajadorYEstado()")
    class BuscarPorTrabajadorYEstado {

        @Test
        @DisplayName("Debe retornar nóminas del trabajador filtradas por el estado indicado")
        void debeRetornarNominasFiltadasPorEstado() {
            when(jpa.findByTrabajadorIdAndEstado(1L, EstadoNomina.PENDIENTE))
                    .thenReturn(List.of(entityBase));

            List<Nomina> resultado =
                    repository.buscarPorTrabajadorYEstado(1L, EstadoNomina.PENDIENTE);

            assertEquals(1, resultado.size());
            assertEquals(EstadoNomina.PENDIENTE, resultado.get(0).getEstado());
            assertEquals(1L, resultado.get(0).getTrabajador().getId());
            verify(jpa, times(1)).findByTrabajadorIdAndEstado(1L, EstadoNomina.PENDIENTE);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si el trabajador no tiene nóminas con ese estado")
        void debeRetornarListaVaciaSiNoHayNominasConEseEstado() {
            when(jpa.findByTrabajadorIdAndEstado(1L, EstadoNomina.APROBADA))
                    .thenReturn(List.of());

            List<Nomina> resultado =
                    repository.buscarPorTrabajadorYEstado(1L, EstadoNomina.APROBADA);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByTrabajadorIdAndEstado(1L, EstadoNomina.APROBADA);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorPeriodo
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorPeriodo()")
    class BuscarPorPeriodo {

        @Test
        @DisplayName("Debe retornar nóminas cuyo periodo coincide con el rango indicado")
        void debeRetornarNominasDelPeriodo() {
            when(jpa.findByPeriodo(INICIO, FIN)).thenReturn(List.of(entityBase));

            List<Nomina> resultado = repository.buscarPorPeriodo(INICIO, FIN);

            assertEquals(1,      resultado.size());
            assertEquals(INICIO, resultado.get(0).getPeriodoInicio());
            assertEquals(FIN,    resultado.get(0).getPeriodoFin());
            verify(jpa, times(1)).findByPeriodo(INICIO, FIN);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay nóminas en el periodo")
        void debeRetornarListaVaciaSiNoHayNominasEnElPeriodo() {
            when(jpa.findByPeriodo(INICIO, FIN)).thenReturn(List.of());

            List<Nomina> resultado = repository.buscarPorPeriodo(INICIO, FIN);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByPeriodo(INICIO, FIN);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  listarTodas
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarTodas()")
    class ListarTodas {

        @Test
        @DisplayName("Debe retornar todas las nóminas mapeadas al dominio")
        void debeRetornarTodasLasNominas() {
            NominaEntity otraEntity = new NominaEntity();
            otraEntity.setId(2L);
            otraEntity.setTrabajador(entityBase.getTrabajador());
            otraEntity.setPeriodoInicio(LocalDate.of(2025, 2, 1));
            otraEntity.setPeriodoFin(LocalDate.of(2025, 2, 28));
            otraEntity.setTotalJornales(0);
            otraEntity.setValorTotal(BigDecimal.ZERO);
            otraEntity.setEstado(EstadoNomina.APROBADA);
            otraEntity.setJornales(List.of());

            when(jpa.findAll()).thenReturn(List.of(entityBase, otraEntity));

            List<Nomina> resultado = repository.listarTodas();

            assertEquals(2,                      resultado.size());
            assertEquals(EstadoNomina.PENDIENTE, resultado.get(0).getEstado());
            assertEquals(EstadoNomina.APROBADA,  resultado.get(1).getEstado());
            verify(jpa, times(1)).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay nóminas registradas")
        void debeRetornarListaVaciaSiNoHayNominas() {
            when(jpa.findAll()).thenReturn(List.of());

            List<Nomina> resultado = repository.listarTodas();

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findAll();
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