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
@DisplayName("PagoRepositoryImpl - Pruebas Unitarias")
class PagoRepositoryImplTest {

    @Mock
    private JpaPagoRepository jpa;

    @InjectMocks
    private PagoRepositoryImpl repository;

    private Pago pagoBase;
    private PagoEntity entityBase;

    private final LocalDate FECHA_PAGO = LocalDate.of(2025, 1, 31);
    private final LocalDate INICIO     = LocalDate.of(2025, 1, 1);
    private final LocalDate FIN        = LocalDate.of(2025, 1, 31);

    @BeforeEach
    void setUp() {
        // ── dominio ────────────────────────────────────────────────────────
        Cargo cargo = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);
        Trabajador trabajador = new Trabajador(
                1L, "Juan", "Pérez", "123456789", "3001234567", "Calle 1",
                LocalDate.of(2020, 1, 1), TipoContrato.JORNAL, cargo
        );
        Nomina nomina = new Nomina(1L, trabajador, INICIO, FIN, List.of());
        nomina.setEstado(EstadoNomina.APROBADA);

        pagoBase = new Pago(1L, nomina, FECHA_PAGO,
                new BigDecimal("1600000"), MetodoPago.EFECTIVO, "COMP-001");
        pagoBase.setObservaciones("Pago enero");

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

        NominaEntity nominaEntity = new NominaEntity();
        nominaEntity.setId(1L);
        nominaEntity.setTrabajador(trabajadorEntity);
        nominaEntity.setPeriodoInicio(INICIO);
        nominaEntity.setPeriodoFin(FIN);
        nominaEntity.setTotalJornales(0);
        nominaEntity.setValorTotal(BigDecimal.ZERO);
        nominaEntity.setEstado(EstadoNomina.APROBADA);
        nominaEntity.setJornales(List.of());

        entityBase = new PagoEntity();
        entityBase.setId(1L);
        entityBase.setNomina(nominaEntity);
        entityBase.setFechaPago(FECHA_PAGO);
        entityBase.setMonto(new BigDecimal("1600000"));
        entityBase.setMetodoPago(MetodoPago.EFECTIVO);
        entityBase.setComprobante("COMP-001");
        entityBase.setObservaciones("Pago enero");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  guardar
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("guardar()")
    class Guardar {

        @Test
        @DisplayName("Debe guardar y retornar el pago mapeado al dominio")
        void debeGuardarYRetornarPagoMapeado() {
            when(jpa.save(any(PagoEntity.class))).thenReturn(entityBase);

            Pago resultado = repository.guardar(pagoBase);

            assertNotNull(resultado);
            assertEquals(1L,                        resultado.getId());
            assertEquals(FECHA_PAGO,                resultado.getFechaPago());
            assertEquals(new BigDecimal("1600000"), resultado.getMonto());
            assertEquals(MetodoPago.EFECTIVO,       resultado.getMetodoPago());
            assertEquals("COMP-001",                resultado.getComprobante());
            assertEquals("Pago enero",              resultado.getObservaciones());
            assertEquals(1L,                        resultado.getNomina().getId());
            verify(jpa, times(1)).save(any(PagoEntity.class));
        }

        @Test
        @DisplayName("Debe guardar correctamente un pago con método de pago TRANSFERENCIA")
        void debeGuardarPagoConMetodoTransferencia() {
            PagoEntity entityTransferencia = new PagoEntity();
            entityTransferencia.setId(2L);
            entityTransferencia.setNomina(entityBase.getNomina());
            entityTransferencia.setFechaPago(FECHA_PAGO);
            entityTransferencia.setMonto(new BigDecimal("2400000"));
            entityTransferencia.setMetodoPago(MetodoPago.TRANSFERENCIA);
            entityTransferencia.setComprobante("TRF-002");

            Pago pagoTransferencia = new Pago(2L, pagoBase.getNomina(), FECHA_PAGO,
                    new BigDecimal("2400000"), MetodoPago.TRANSFERENCIA, "TRF-002");

            when(jpa.save(any(PagoEntity.class))).thenReturn(entityTransferencia);

            Pago resultado = repository.guardar(pagoTransferencia);

            assertNotNull(resultado);
            assertEquals(2L,                        resultado.getId());
            assertEquals(MetodoPago.TRANSFERENCIA,  resultado.getMetodoPago());
            assertEquals(new BigDecimal("2400000"), resultado.getMonto());
            assertEquals("TRF-002",                 resultado.getComprobante());
            verify(jpa, times(1)).save(any(PagoEntity.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorId
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Debe retornar Optional con el pago mapeado cuando el id existe")
        void debeRetornarOptionalConPagoSiIdExiste() {
            when(jpa.findById(1L)).thenReturn(Optional.of(entityBase));

            Optional<Pago> resultado = repository.buscarPorId(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,                        resultado.get().getId());
            assertEquals(FECHA_PAGO,                resultado.get().getFechaPago());
            assertEquals(new BigDecimal("1600000"), resultado.get().getMonto());
            assertEquals(MetodoPago.EFECTIVO,        resultado.get().getMetodoPago());
            assertEquals(1L,                        resultado.get().getNomina().getId());
            verify(jpa, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el id no existe")
        void debeRetornarOptionalVacioSiIdNoExiste() {
            when(jpa.findById(99L)).thenReturn(Optional.empty());

            Optional<Pago> resultado = repository.buscarPorId(99L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findById(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorNomina
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorNomina()")
    class BuscarPorNomina {

        @Test
        @DisplayName("Debe retornar Optional con el pago cuando la nómina tiene pago registrado")
        void debeRetornarPagoCuandoNominaTienePago() {
            when(jpa.findByNominaId(1L)).thenReturn(Optional.of(entityBase));

            Optional<Pago> resultado = repository.buscarPorNomina(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,         resultado.get().getId());
            assertEquals(1L,         resultado.get().getNomina().getId());
            assertEquals(FECHA_PAGO, resultado.get().getFechaPago());
            verify(jpa, times(1)).findByNominaId(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando la nómina no tiene pago registrado")
        void debeRetornarOptionalVacioSiNominaSinPago() {
            when(jpa.findByNominaId(99L)).thenReturn(Optional.empty());

            Optional<Pago> resultado = repository.buscarPorNomina(99L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByNominaId(99L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  buscarPorTrabajador
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorTrabajador()")
    class BuscarPorTrabajador {

        @Test
        @DisplayName("Debe retornar los pagos del trabajador mapeados al dominio")
        void debeRetornarPagosDelTrabajador() {
            when(jpa.findByTrabajadorId(1L)).thenReturn(List.of(entityBase));

            List<Pago> resultado = repository.buscarPorTrabajador(1L);

            assertEquals(1,                    resultado.size());
            assertEquals(1L,                   resultado.get(0).getId());
            assertEquals(1L,                   resultado.get(0).getNomina().getId());
            assertEquals(MetodoPago.EFECTIVO,  resultado.get(0).getMetodoPago());
            verify(jpa, times(1)).findByTrabajadorId(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si el trabajador no tiene pagos registrados")
        void debeRetornarListaVaciaSiTrabajadorSinPagos() {
            when(jpa.findByTrabajadorId(1L)).thenReturn(List.of());

            List<Pago> resultado = repository.buscarPorTrabajador(1L);

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findByTrabajadorId(1L);
        }

        @Test
        @DisplayName("Debe retornar múltiples pagos cuando el trabajador tiene varios registros")
        void debeRetornarMultiplesPagosDelTrabajador() {
            PagoEntity segundaEntity = new PagoEntity();
            segundaEntity.setId(2L);
            segundaEntity.setNomina(entityBase.getNomina());
            segundaEntity.setFechaPago(LocalDate.of(2025, 2, 28));
            segundaEntity.setMonto(new BigDecimal("1800000"));
            segundaEntity.setMetodoPago(MetodoPago.TRANSFERENCIA);
            segundaEntity.setComprobante("TRF-002");

            when(jpa.findByTrabajadorId(1L)).thenReturn(List.of(entityBase, segundaEntity));

            List<Pago> resultado = repository.buscarPorTrabajador(1L);

            assertEquals(2,                        resultado.size());
            assertEquals(MetodoPago.EFECTIVO,      resultado.get(0).getMetodoPago());
            assertEquals(MetodoPago.TRANSFERENCIA, resultado.get(1).getMetodoPago());
            verify(jpa, times(1)).findByTrabajadorId(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  listarTodos
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Debe retornar todos los pagos mapeados al dominio")
        void debeRetornarTodosLosPagos() {
            PagoEntity otraEntity = new PagoEntity();
            otraEntity.setId(2L);
            otraEntity.setNomina(entityBase.getNomina());
            otraEntity.setFechaPago(LocalDate.of(2025, 2, 28));
            otraEntity.setMonto(new BigDecimal("2000000"));
            otraEntity.setMetodoPago(MetodoPago.TRANSFERENCIA);
            otraEntity.setComprobante("TRF-002");

            when(jpa.findAll()).thenReturn(List.of(entityBase, otraEntity));

            List<Pago> resultado = repository.listarTodos();

            assertEquals(2,                        resultado.size());
            assertEquals(1L,                       resultado.get(0).getId());
            assertEquals(2L,                       resultado.get(1).getId());
            assertEquals(MetodoPago.EFECTIVO,      resultado.get(0).getMetodoPago());
            assertEquals(MetodoPago.TRANSFERENCIA, resultado.get(1).getMetodoPago());
            verify(jpa, times(1)).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay pagos registrados")
        void debeRetornarListaVaciaSiNoHayPagos() {
            when(jpa.findAll()).thenReturn(List.of());

            List<Pago> resultado = repository.listarTodos();

            assertTrue(resultado.isEmpty());
            verify(jpa, times(1)).findAll();
        }
    }
}