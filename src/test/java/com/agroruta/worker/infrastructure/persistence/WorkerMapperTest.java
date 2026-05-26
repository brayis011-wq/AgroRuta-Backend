package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkerMapper")
class WorkerMapperTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private CargoEntity cargoEntity() {
        CargoEntity e = new CargoEntity();
        e.setId(1L);
        e.setNombre("Operario de campo");
        e.setDescripcion("Labores agrícolas generales");
        e.setValorJornal(new BigDecimal("85000.00"));
        e.setActivo(true);
        return e;
    }

    private Cargo cargoDomain() {
        return new Cargo(1L, "Operario de campo", "Labores agrícolas generales",
                new BigDecimal("85000.00"), true);
    }

    private ActividadEntity actividadEntity() {
        ActividadEntity e = new ActividadEntity();
        e.setId(2L);
        e.setNombre("Fumigación");
        e.setDescripcion("Aplicación de agroquímicos");
        e.setActiva(true);
        return e;
    }

    private Actividad actividadDomain() {
        // El constructor fija activa=true por defecto
        return new Actividad(2L, "Fumigación", "Aplicación de agroquímicos");
    }

    private TrabajadorEntity trabajadorEntity() {
        TrabajadorEntity e = new TrabajadorEntity();
        e.setId(3L);
        e.setNombre("Carlos");
        e.setApellido("Ramírez");
        e.setCedula("1098765432");
        e.setTelefono("3001234567");
        e.setDireccion("Vereda El Rosal");
        e.setFechaIngreso(LocalDate.of(2023, 1, 15));
        e.setTipoContrato(TipoContrato.JORNAL);
        e.setEstado(EstadoTrabajador.ACTIVO);
        e.setCargo(cargoEntity());
        return e;
    }

    private Trabajador trabajadorDomain() {
        // El constructor fija estado=ACTIVO por defecto
        return new Trabajador(3L, "Carlos", "Ramírez", "1098765432",
                "3001234567", "Vereda El Rosal",
                LocalDate.of(2023, 1, 15),
                TipoContrato.JORNAL,
                cargoDomain());
    }

    private JornalEntity jornalEntity() {
        JornalEntity e = new JornalEntity();
        e.setId(4L);
        e.setFecha(LocalDate.of(2024, 6, 10));
        e.setTrabajador(trabajadorEntity());
        e.setCultivoId(10L);
        e.setNombreCultivo("Café");
        e.setObservaciones("Sin novedad");
        e.setValorJornal(new BigDecimal("85000.00"));
        e.setLiquidado(false);
        e.setActividades(new ArrayList<>(List.of(actividadEntity())));
        return e;
    }

    private Jornal jornalDomain() {
        // El constructor toma valorJornal del cargo del trabajador
        Jornal j = new Jornal(4L, LocalDate.of(2024, 6, 10),
                trabajadorDomain(), 10L, "Café", "Sin novedad");
        j.setActividades(new ArrayList<>(List.of(actividadDomain())));
        return j;
    }

    private NominaEntity nominaEntity() {
        NominaEntity e = new NominaEntity();
        e.setId(5L);
        e.setTrabajador(trabajadorEntity());
        e.setPeriodoInicio(LocalDate.of(2024, 6, 1));
        e.setPeriodoFin(LocalDate.of(2024, 6, 30));
        e.setTotalJornales(1);
        e.setValorTotal(new BigDecimal("85000.00"));
        e.setEstado(EstadoNomina.PENDIENTE);
        e.setFechaGeneracion(LocalDate.of(2024, 7, 1));
        e.setObservaciones("Primera quincena");
        e.setJornales(new ArrayList<>(List.of(jornalEntity())));
        return e;
    }

    private Nomina nominaDomain() {
        // El constructor llama calcular() y fija fechaGeneracion=now()
        Nomina n = new Nomina(5L, trabajadorDomain(),
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30),
                new ArrayList<>(List.of(jornalDomain())));
        n.setObservaciones("Primera quincena");
        return n;
    }

    private PagoEntity pagoEntity() {
        PagoEntity e = new PagoEntity();
        e.setId(6L);
        e.setNomina(nominaEntity());
        e.setFechaPago(LocalDate.of(2024, 7, 5));
        e.setMonto(new BigDecimal("85000.00"));
        e.setMetodoPago(MetodoPago.TRANSFERENCIA);
        e.setComprobante("TRF-2024-001");
        e.setObservaciones("Pago quincenal");
        return e;
    }

    private Pago pagoDomain() {
        Pago p = new Pago(6L, nominaDomain(),
                LocalDate.of(2024, 7, 5),
                new BigDecimal("85000.00"),
                MetodoPago.TRANSFERENCIA,
                "TRF-2024-001");
        p.setObservaciones("Pago quincenal");
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cargo
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cargo")
    class CargoTests {

        @Test
        @DisplayName("toDomain(null) retorna null")
        void toDomain_null() {
            assertNull(WorkerMapper.toDomain((CargoEntity) null));
        }

        @Test
        @DisplayName("toEntity(null) retorna null")
        void toEntity_null() {
            assertNull(WorkerMapper.toEntity((Cargo) null));
        }

        @Test
        @DisplayName("toDomain mapea todos los campos")
        void toDomain_mapea_campos() {
            CargoEntity e = cargoEntity();
            Cargo d = WorkerMapper.toDomain(e);

            assertNotNull(d);
            assertEquals(e.getId(), d.getId());
            assertEquals(e.getNombre(), d.getNombre());
            assertEquals(e.getDescripcion(), d.getDescripcion());
            assertEquals(0, e.getValorJornal().compareTo(d.getValorJornal()));
            assertEquals(e.isActivo(), d.isActivo());
        }

        @Test
        @DisplayName("toEntity mapea todos los campos")
        void toEntity_mapea_campos() {
            Cargo d = cargoDomain();
            CargoEntity e = WorkerMapper.toEntity(d);

            assertNotNull(e);
            assertEquals(d.getId(), e.getId());
            assertEquals(d.getNombre(), e.getNombre());
            assertEquals(d.getDescripcion(), e.getDescripcion());
            assertEquals(0, d.getValorJornal().compareTo(e.getValorJornal()));
            assertEquals(d.isActivo(), e.isActivo());
        }

        @Test
        @DisplayName("round-trip entity → domain → entity conserva id y nombre")
        void roundTrip() {
            CargoEntity original = cargoEntity();
            CargoEntity result = WorkerMapper.toEntity(WorkerMapper.toDomain(original));
            assertEquals(original.getId(), result.getId());
            assertEquals(original.getNombre(), result.getNombre());
        }

        @Test
        @DisplayName("cargo inactivo conserva activo=false")
        void toDomain_inactivo() {
            CargoEntity e = cargoEntity();
            e.setActivo(false);
            assertFalse(WorkerMapper.toDomain(e).isActivo());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Actividad
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Actividad")
    class ActividadTests {

        @Test
        @DisplayName("toDomain(null) retorna null")
        void toDomain_null() {
            assertNull(WorkerMapper.toDomain((ActividadEntity) null));
        }

        @Test
        @DisplayName("toEntity(null) retorna null")
        void toEntity_null() {
            assertNull(WorkerMapper.toEntity((Actividad) null));
        }

        @Test
        @DisplayName("toDomain mapea todos los campos")
        void toDomain_mapea_campos() {
            ActividadEntity e = actividadEntity();
            Actividad d = WorkerMapper.toDomain(e);

            assertNotNull(d);
            assertEquals(e.getId(), d.getId());
            assertEquals(e.getNombre(), d.getNombre());
            assertEquals(e.getDescripcion(), d.getDescripcion());
            assertEquals(e.isActiva(), d.isActiva());
        }

        @Test
        @DisplayName("toEntity mapea todos los campos")
        void toEntity_mapea_campos() {
            Actividad d = actividadDomain();
            ActividadEntity e = WorkerMapper.toEntity(d);

            assertNotNull(e);
            assertEquals(d.getId(), e.getId());
            assertEquals(d.getNombre(), e.getNombre());
            assertEquals(d.getDescripcion(), e.getDescripcion());
            assertEquals(d.isActiva(), e.isActiva());
        }

        @Test
        @DisplayName("actividad inactiva conserva activa=false")
        void toDomain_inactiva() {
            ActividadEntity e = actividadEntity();
            e.setActiva(false);
            assertFalse(WorkerMapper.toDomain(e).isActiva());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Trabajador
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Trabajador")
    class TrabajadorTests {

        @Test
        @DisplayName("toDomain(null) retorna null")
        void toDomain_null() {
            assertNull(WorkerMapper.toDomain((TrabajadorEntity) null));
        }

        @Test
        @DisplayName("toEntity(null) retorna null")
        void toEntity_null() {
            assertNull(WorkerMapper.toEntity((Trabajador) null));
        }

        @Test
        @DisplayName("toDomain mapea campos primitivos")
        void toDomain_mapea_campos_primitivos() {
            TrabajadorEntity e = trabajadorEntity();
            Trabajador d = WorkerMapper.toDomain(e);

            assertNotNull(d);
            assertEquals(e.getId(), d.getId());
            assertEquals(e.getNombre(), d.getNombre());
            assertEquals(e.getApellido(), d.getApellido());
            assertEquals(e.getCedula(), d.getCedula());
            assertEquals(e.getTelefono(), d.getTelefono());
            assertEquals(e.getDireccion(), d.getDireccion());
            assertEquals(e.getFechaIngreso(), d.getFechaIngreso());
            assertEquals(e.getTipoContrato(), d.getTipoContrato());
            assertEquals(e.getEstado(), d.getEstado());
        }

        @Test
        @DisplayName("toDomain mapea el Cargo anidado")
        void toDomain_mapea_cargo_anidado() {
            TrabajadorEntity e = trabajadorEntity();
            Trabajador d = WorkerMapper.toDomain(e);

            assertNotNull(d.getCargo());
            assertEquals(e.getCargo().getId(), d.getCargo().getId());
            assertEquals(e.getCargo().getNombre(), d.getCargo().getNombre());
        }

        @Test
        @DisplayName("toDomain con cargo null no lanza excepción")
        void toDomain_cargo_null() {
            TrabajadorEntity e = trabajadorEntity();
            e.setCargo(null);
            Trabajador d = WorkerMapper.toDomain(e);
            assertNotNull(d);
            assertNull(d.getCargo());
        }

        @Test
        @DisplayName("toEntity mapea TipoContrato y EstadoTrabajador como enum")
        void toEntity_mapea_enums() {
            Trabajador d = trabajadorDomain();
            TrabajadorEntity e = WorkerMapper.toEntity(d);

            assertEquals(TipoContrato.JORNAL, e.getTipoContrato());
            assertEquals(EstadoTrabajador.ACTIVO, e.getEstado());
        }

        @Test
        @DisplayName("toEntity mapea el CargoEntity anidado")
        void toEntity_mapea_cargo_anidado() {
            Trabajador d = trabajadorDomain();
            TrabajadorEntity e = WorkerMapper.toEntity(d);

            assertNotNull(e.getCargo());
            assertEquals(d.getCargo().getId(), e.getCargo().getId());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Jornal
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Jornal")
    class JornalTests {

        @Test
        @DisplayName("toDomain(null) retorna null")
        void toDomain_null() {
            assertNull(WorkerMapper.toDomain((JornalEntity) null));
        }

        @Test
        @DisplayName("toEntity(null) retorna null")
        void toEntity_null() {
            assertNull(WorkerMapper.toEntity((Jornal) null));
        }

        @Test
        @DisplayName("toDomain mapea campos directos")
        void toDomain_mapea_campos() {
            JornalEntity e = jornalEntity();
            Jornal d = WorkerMapper.toDomain(e);

            assertNotNull(d);
            assertEquals(e.getId(), d.getId());
            assertEquals(e.getFecha(), d.getFecha());
            assertEquals(e.getCultivoId(), d.getCultivoId());
            assertEquals(e.getNombreCultivo(), d.getNombreCultivo());
            assertEquals(e.getObservaciones(), d.getObservaciones());
            assertEquals(0, e.getValorJornal().compareTo(d.getValorJornal()));
            assertEquals(e.isLiquidado(), d.isLiquidado());
        }

        @Test
        @DisplayName("toDomain mapea el Trabajador anidado")
        void toDomain_mapea_trabajador_anidado() {
            JornalEntity e = jornalEntity();
            Jornal d = WorkerMapper.toDomain(e);

            assertNotNull(d.getTrabajador());
            assertEquals(e.getTrabajador().getId(), d.getTrabajador().getId());
        }

        @Test
        @DisplayName("toDomain convierte la lista de actividades")
        void toDomain_mapea_actividades() {
            JornalEntity e = jornalEntity();
            Jornal d = WorkerMapper.toDomain(e);

            assertEquals(1, d.getActividades().size());
            assertEquals(e.getActividades().get(0).getId(),
                    d.getActividades().get(0).getId());
        }

        @Test
        @DisplayName("toDomain con lista de actividades vacía no lanza excepción")
        void toDomain_actividades_vacias() {
            JornalEntity e = jornalEntity();
            e.setActividades(new ArrayList<>());
            Jornal d = WorkerMapper.toDomain(e);
            assertTrue(d.getActividades().isEmpty());
        }

        @Test
        @DisplayName("jornal liquidado conserva liquidado=true")
        void toDomain_liquidado() {
            JornalEntity e = jornalEntity();
            e.setLiquidado(true);
            assertTrue(WorkerMapper.toDomain(e).isLiquidado());
        }

        @Test
        @DisplayName("toEntity mapea campos directos")
        void toEntity_mapea_campos() {
            Jornal d = jornalDomain();
            JornalEntity e = WorkerMapper.toEntity(d);

            assertNotNull(e);
            assertEquals(d.getId(), e.getId());
            assertEquals(d.getFecha(), e.getFecha());
            assertEquals(d.getCultivoId(), e.getCultivoId());
            assertEquals(d.getNombreCultivo(), e.getNombreCultivo());
            assertEquals(d.getObservaciones(), e.getObservaciones());
            assertEquals(0, d.getValorJornal().compareTo(e.getValorJornal()));
            assertEquals(d.isLiquidado(), e.isLiquidado());
        }

        @Test
        @DisplayName("toEntity convierte la lista de actividades")
        void toEntity_mapea_actividades() {
            Jornal d = jornalDomain();
            JornalEntity e = WorkerMapper.toEntity(d);

            assertEquals(1, e.getActividades().size());
            assertEquals(d.getActividades().get(0).getId(),
                    e.getActividades().get(0).getId());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Nomina
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Nomina")
    class NominaTests {

        @Test
        @DisplayName("toDomain(null) retorna null")
        void toDomain_null() {
            assertNull(WorkerMapper.toDomain((NominaEntity) null));
        }

        @Test
        @DisplayName("toEntity(null) retorna null")
        void toEntity_null() {
            assertNull(WorkerMapper.toEntity((Nomina) null));
        }

        @Test
        @DisplayName("toDomain mapea campos directos")
        void toDomain_mapea_campos() {
            NominaEntity e = nominaEntity();
            Nomina d = WorkerMapper.toDomain(e);

            assertNotNull(d);
            assertEquals(e.getId(), d.getId());
            assertEquals(e.getPeriodoInicio(), d.getPeriodoInicio());
            assertEquals(e.getPeriodoFin(), d.getPeriodoFin());
            assertEquals(e.getEstado(), d.getEstado());
            assertEquals(e.getFechaGeneracion(), d.getFechaGeneracion());
            assertEquals(e.getObservaciones(), d.getObservaciones());
        }

        @Test
        @DisplayName("toDomain mapea el Trabajador anidado")
        void toDomain_mapea_trabajador_anidado() {
            NominaEntity e = nominaEntity();
            Nomina d = WorkerMapper.toDomain(e);

            assertNotNull(d.getTrabajador());
            assertEquals(e.getTrabajador().getId(), d.getTrabajador().getId());
        }

        @Test
        @DisplayName("toDomain convierte la lista de jornales")
        void toDomain_mapea_jornales() {
            NominaEntity e = nominaEntity();
            Nomina d = WorkerMapper.toDomain(e);

            assertEquals(1, d.getJornales().size());
            assertEquals(e.getJornales().get(0).getId(),
                    d.getJornales().get(0).getId());
        }

        @Test
        @DisplayName("toDomain con lista de jornales vacía no lanza excepción")
        void toDomain_jornales_vacios() {
            NominaEntity e = nominaEntity();
            e.setJornales(new ArrayList<>());
            Nomina d = WorkerMapper.toDomain(e);
            assertTrue(d.getJornales().isEmpty());
        }

        @Test
        @DisplayName("toDomain mapea EstadoNomina como enum")
        void toDomain_mapea_estado_enum() {
            NominaEntity e = nominaEntity();
            e.setEstado(EstadoNomina.APROBADA);
            assertEquals(EstadoNomina.APROBADA, WorkerMapper.toDomain(e).getEstado());
        }

        @Test
        @DisplayName("toEntity persiste totalJornales y valorTotal calculados por el dominio")
        void toEntity_persiste_totales() {
            Nomina d = nominaDomain(); // calcular() ya fue llamado en el constructor
            NominaEntity e = WorkerMapper.toEntity(d);

            assertEquals(d.getTotalJornales(), e.getTotalJornales());
            assertEquals(0, d.getValorTotal().compareTo(e.getValorTotal()));
        }

        @Test
        @DisplayName("toEntity mapea la lista de JornalEntity")
        void toEntity_mapea_jornales() {
            Nomina d = nominaDomain();
            NominaEntity e = WorkerMapper.toEntity(d);

            assertEquals(d.getJornales().size(), e.getJornales().size());
            assertEquals(d.getJornales().get(0).getId(),
                    e.getJornales().get(0).getId());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Pago
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Pago")
    class PagoTests {

        @Test
        @DisplayName("toDomain(null) retorna null")
        void toDomain_null() {
            assertNull(WorkerMapper.toDomain((PagoEntity) null));
        }

        @Test
        @DisplayName("toEntity(null) retorna null")
        void toEntity_null() {
            assertNull(WorkerMapper.toEntity((Pago) null));
        }

        @Test
        @DisplayName("toDomain mapea todos los campos")
        void toDomain_mapea_campos() {
            PagoEntity e = pagoEntity();
            Pago d = WorkerMapper.toDomain(e);

            assertNotNull(d);
            assertEquals(e.getId(), d.getId());
            assertEquals(e.getFechaPago(), d.getFechaPago());
            assertEquals(0, e.getMonto().compareTo(d.getMonto()));
            assertEquals(e.getMetodoPago(), d.getMetodoPago());
            assertEquals(e.getComprobante(), d.getComprobante());
            assertEquals(e.getObservaciones(), d.getObservaciones());
        }

        @Test
        @DisplayName("toDomain mapea MetodoPago como enum")
        void toDomain_mapea_metodo_pago_enum() {
            PagoEntity e = pagoEntity();
            e.setMetodoPago(MetodoPago.EFECTIVO);
            assertEquals(MetodoPago.EFECTIVO, WorkerMapper.toDomain(e).getMetodoPago());
        }

        @Test
        @DisplayName("toDomain mapea la Nomina anidada")
        void toDomain_mapea_nomina_anidada() {
            PagoEntity e = pagoEntity();
            Pago d = WorkerMapper.toDomain(e);

            assertNotNull(d.getNomina());
            assertEquals(e.getNomina().getId(), d.getNomina().getId());
        }

        @Test
        @DisplayName("toDomain con nomina null no lanza excepción")
        void toDomain_nomina_null() {
            PagoEntity e = pagoEntity();
            e.setNomina(null);
            Pago d = WorkerMapper.toDomain(e);
            assertNotNull(d);
            assertNull(d.getNomina());
        }

        @Test
        @DisplayName("toEntity mapea todos los campos")
        void toEntity_mapea_campos() {
            Pago d = pagoDomain();
            PagoEntity e = WorkerMapper.toEntity(d);

            assertNotNull(e);
            assertEquals(d.getId(), e.getId());
            assertEquals(d.getFechaPago(), e.getFechaPago());
            assertEquals(0, d.getMonto().compareTo(e.getMonto()));
            assertEquals(d.getMetodoPago(), e.getMetodoPago());
            assertEquals(d.getComprobante(), e.getComprobante());
            assertEquals(d.getObservaciones(), e.getObservaciones());
        }

        @Test
        @DisplayName("toEntity mapea la NominaEntity anidada")
        void toEntity_mapea_nomina_anidada() {
            Pago d = pagoDomain();
            PagoEntity e = WorkerMapper.toEntity(d);

            assertNotNull(e.getNomina());
            assertEquals(d.getNomina().getId(), e.getNomina().getId());
        }
    }
}