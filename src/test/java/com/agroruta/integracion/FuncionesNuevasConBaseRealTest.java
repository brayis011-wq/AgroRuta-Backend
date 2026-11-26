package com.agroruta.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import com.agroruta.crop.application.ports.out.ActividadProgramadaRepository;
import com.agroruta.crop.application.ports.out.LluviaRepository;
import com.agroruta.crop.application.ports.out.PresupuestoRepository;
import com.agroruta.crop.domain.ActividadProgramada;
import com.agroruta.crop.domain.PresupuestoSiembra;
import com.agroruta.finance.application.ports.out.GastosFincaRepository;
import com.agroruta.finance.application.ports.out.PreciosReferenciaRepository;
import com.agroruta.finance.application.ports.out.VentasRepository;
import com.agroruta.finance.domain.GastoFinca;
import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.finance.domain.PreferenciaPrecio;
import com.agroruta.finance.domain.Venta;
import com.agroruta.report.application.ports.out.CampoQueryPort;
import com.agroruta.report.domain.Periodo;
import com.agroruta.worker.application.ports.out.JornalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Las consultas SQL de las funciones de V19 contra un MySQL real. */
@Transactional
class FuncionesNuevasConBaseRealTest extends ConBaseReal {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);

    @Autowired
    private GastosFincaRepository gastos;

    @Autowired
    private VentasRepository ventas;

    @Autowired
    private PreciosReferenciaRepository precios;

    @Autowired
    private LluviaRepository lluvias;

    @Autowired
    private ActividadProgramadaRepository programadas;

    @Autowired
    private PresupuestoRepository presupuestos;

    @Autowired
    private CampoQueryPort campo;

    @Autowired
    private JornalRepository jornales;

    private long ana;
    private long finca;
    private long siembra;

    @BeforeEach
    void datos() {
        ana = insertar(
                "INSERT INTO usuarios (activo, email, fecha_creacion, nombre, password, rol) VALUES (1, ?, ?, 'Ana', 'x', 'Agricultor')",
                "ana-" + System.nanoTime() + "@agroruta.test",
                LocalDateTime.now());
        finca = insertar(
                "INSERT INTO fincas (agricultor_id, fecha_registro, hectareas, nombre, ubicacion) VALUES (?, ?, 3, 'La Esperanza', 'Subia')",
                ana,
                LocalDateTime.now());
        long lote = insertar(
                "INSERT INTO lotes (area, estado, finca_id, nombre) VALUES (1.5, 'EN_CULTIVO', ?, 'Lote 1')", finca);
        siembra = insertar(
                "INSERT INTO siembras (cantidad_plantas, estado_cultivo, fecha_siembra, lote_id, variedad, tipo_cultivo) "
                        + "VALUES (100, 'PRODUCCION', ?, ?, 'Castillo', 'Café')",
                DIA.minusDays(90),
                lote);
    }

    @Test
    void gastosGeneralesSeListanYSeResumenPorCategoria() {
        gastos.crear(new GastoFinca(
                null, finca, null, DIA, GastoFinca.Categoria.TRANSPORTE, "Flete", new BigDecimal("80000")));
        gastos.crear(new GastoFinca(
                null, finca, null, DIA.plusDays(1), GastoFinca.Categoria.TRANSPORTE, "Flete", new BigDecimal("20000")));
        gastos.crear(new GastoFinca(
                null, finca, null, DIA, GastoFinca.Categoria.HERRAMIENTAS, "Machete", new BigDecimal("35000")));

        assertThat(gastos.listar(ana, null, null, null))
                .hasSize(3)
                .first()
                .satisfies(g -> assertThat(g.fincaNombre()).isEqualTo("La Esperanza"));
        var r = gastos.resumen(ana, DIA, DIA.plusDays(1));
        assertThat(r.total()).isEqualByComparingTo("135000");
        assertThat(r.porCategoria().get(0).clave()).isEqualTo("TRANSPORTE");
        assertThat(campo.gastosGenerales(ana, Periodo.de(DIA, DIA)).total()).isEqualTo(115000);
    }

    @Test
    void ventasConAbonosDanElSaldo() {
        Venta v = ventas.crear(
                ana,
                Venta.nueva(
                        null,
                        siembra,
                        "Cooperativa",
                        DIA,
                        "Café pergamino",
                        new BigDecimal("100"),
                        new BigDecimal("18000"),
                        null,
                        DIA.plusDays(15),
                        null));
        ventas.agregarAbono(new Venta.Abono(null, v.id(), DIA.plusDays(2), new BigDecimal("500000"), "EFECTIVO", null));

        Venta leida = ventas.buscarPorId(v.id()).orElseThrow();
        assertThat(leida.total()).isEqualByComparingTo("1800000");
        assertThat(leida.saldo()).isEqualByComparingTo("1300000");
        assertThat(leida.abonos()).hasSize(1);
        assertThat(leida.siembraNombre()).contains("Café").contains("Lote 1");
        assertThat(ventas.listar(ana, null, null))
                .singleElement()
                .satisfies(x -> assertThat(x.abonado()).isEqualByComparingTo("500000"));
        assertThat(ventas.cobradoEntre(ana, DIA, DIA.plusDays(5))).isEqualByComparingTo("500000");
        assertThat(ventas.compradores(ana)).containsExactly("Cooperativa");
    }

    @Test
    void preciosDeReferenciaYPreferencias() {
        LocalDate semana = LocalDate.of(2026, 11, 14);
        precios.guardar(
                List.of(
                        new PrecioReferencia("Lulo", semana.minusWeeks(1), new BigDecimal("3000"), null, null, 4),
                        new PrecioReferencia(
                                "Lulo",
                                semana,
                                new BigDecimal("3500"),
                                new BigDecimal("2800"),
                                new BigDecimal("4100"),
                                5)),
                LocalDateTime.now());
        assertThat(precios.ultimaSemana())
                .singleElement()
                .satisfies(p -> assertThat(p.precioKg()).isEqualByComparingTo("3500"));
        assertThat(precios.ultimaActualizacion()).isPresent();

        precios.guardarPreferencia(
                ana, new PreferenciaPrecio("Café", null, new BigDecimal("17500")), LocalDateTime.now());
        precios.guardarPreferencia(
                ana, new PreferenciaPrecio("Café", null, new BigDecimal("18000")), LocalDateTime.now());
        assertThat(precios.preferencia(ana, "Café"))
                .get()
                .satisfies(p -> assertThat(p.precioKg()).isEqualByComparingTo("18000"));
        precios.borrarPreferencia(ana, "Café");
        assertThat(precios.preferencias(ana)).isEmpty();
    }

    @Test
    void lluviaUnDatoPorDia() {
        lluvias.anotar(finca, DIA, new BigDecimal("12.5"));
        lluvias.anotar(finca, DIA, new BigDecimal("15.0"));
        lluvias.anotar(finca, DIA.plusDays(1), BigDecimal.ZERO);
        assertThat(lluvias.deFinca(finca, DIA, DIA.plusDays(3)))
                .hasSize(2)
                .first()
                .satisfies(l -> assertThat(l.milimetros()).isEqualByComparingTo("15.0"));
        lluvias.borrar(finca, DIA);
        assertThat(lluvias.deFinca(finca, DIA, DIA.plusDays(3))).hasSize(1);
    }

    @Test
    void lasLaboresProgramadasVenLoQueSeHizo() {
        long insumo = insertar(
                "INSERT INTO agricultural_inputs (activo, creado_en, dosis_sugerida, nombre, reentrada_horas, tipo, unidad_sugerida) "
                        + "VALUES (1, ?, 2, 'Triple 15', 0, 'FERTILIZANTE_SUELO', 'KG')",
                LocalDateTime.now());
        insertar(
                "INSERT INTO fumigaciones (area_aplicada, dosis, fecha, producto, siembra_id, unidad_medida, agricultural_input_id) "
                        + "VALUES (1, 2, ?, 'Triple 15', ?, 'KG', ?)",
                DIA,
                siembra,
                insumo);
        insertar(
                "INSERT INTO fumigaciones (area_aplicada, dosis, fecha, producto, siembra_id, unidad_medida) "
                        + "VALUES (1, 2, ?, 'Mancozeb', ?, 'KG')",
                DIA.minusDays(3),
                siembra);
        insertar(
                "INSERT INTO actividades_cultivo (descripcion, fecha, siembra_id, tipo) VALUES ('Deshierbe', ?, ?, 'DESHIERBE')",
                DIA,
                siembra);

        ActividadProgramada p =
                programadas.guardar(ActividadProgramada.nueva(null, siembra, "FERTILIZACION", null, 30, DIA, null));
        assertThat(programadas.deSiembra(siembra))
                .singleElement()
                .satisfies(x -> assertThat(x.id()).isEqualTo(p.id()));

        var hechas = programadas.fechasHechas(siembra);
        assertThat(hechas.get("FERTILIZACION")).containsExactly(DIA);
        assertThat(hechas.get("FUMIGACION")).containsExactly(DIA.minusDays(3));
        assertThat(hechas.get("DESHIERBE")).containsExactly(DIA);

        assertThat(campo.aplicaciones(ana, finca, Periodo.TODO))
                .hasSize(2)
                .first()
                .satisfies(a -> assertThat(a.producto()).isEqualTo("Mancozeb"));
    }

    @Test
    void elPresupuestoSeGuardaYSoloAvisaUnaVez() {
        presupuestos.guardar(new PresupuestoSiembra(siembra, new BigDecimal("1000000"), null, null, 0));
        assertThat(presupuestos.activosDe(ana))
                .singleElement()
                .satisfies(c -> assertThat(c.finca()).isEqualTo("La Esperanza"));
        presupuestos.marcarAvisado(siembra, 90);
        presupuestos.marcarAvisado(siembra, 90);
        assertThat(presupuestos.deSiembra(siembra))
                .get()
                .satisfies(p -> assertThat(p.avisado()).isEqualTo(90));
        presupuestos.guardar(new PresupuestoSiembra(siembra, new BigDecimal("2000000"), null, null, 0));
        assertThat(presupuestos.deSiembra(siembra))
                .get()
                .satisfies(p -> assertThat(p.avisado()).isZero());
    }

    @Autowired
    private com.agroruta.worker.application.ports.in.JornalUseCase jornalUseCase;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    @Test
    void lasLaboresTipicasQuedanParaTodosSinRepetirFumigar() {
        Integer comunes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM actividades WHERE agricultor_id IS NULL AND activa = b'1'", Integer.class);
        assertThat(comunes).isGreaterThanOrEqualTo(15);
        Integer recoleccion = jdbc.queryForObject(
                "SELECT COUNT(*) FROM actividades WHERE agricultor_id IS NULL AND nombre = 'Recolección'",
                Integer.class);
        assertThat(recoleccion).isEqualTo(1);
    }

    @Test
    void quitarDeLaPlanillaUnJornalDeUnaNominaPendienteLaRecalcula() {
        long cargo = insertar(
                "INSERT INTO cargos (activo, nombre, valor_jornal, agricultor_id) VALUES (1, 'Jornalero', 60000, ?)",
                ana);
        long t = insertar(
                "INSERT INTO trabajadores (apellido, cedula, estado, fecha_ingreso, nombre, tipo_contrato, cargo_id, agricultor_id) "
                        + "VALUES ('Paz', '77', 'ACTIVO', ?, 'Leo', 'JORNAL', ?, ?)",
                DIA,
                cargo,
                ana);
        long j1 = insertar(
                "INSERT INTO jornales (cultivo_id, fecha, liquidado, valor_jornal, trabajador_id) VALUES (?, ?, 0, 60000, ?)",
                siembra,
                DIA,
                t);
        long j2 = insertar(
                "INSERT INTO jornales (cultivo_id, fecha, liquidado, valor_jornal, trabajador_id) VALUES (?, ?, 0, 60000, ?)",
                siembra,
                DIA.plusDays(1),
                t);
        long n = insertar(
                "INSERT INTO nominas (estado, fecha_generacion, periodo_fin, periodo_inicio, total_jornales, valor_total, trabajador_id) "
                        + "VALUES ('PENDIENTE', ?, ?, ?, 2, 120000, ?)",
                DIA,
                DIA.plusDays(5),
                DIA,
                t);
        jdbc.update("INSERT INTO nomina_jornales (nomina_id, jornal_id) VALUES (?, ?), (?, ?)", n, j1, n, j2);

        jornalUseCase.eliminarJornal(j1);
        em.flush();

        assertThat(jdbc.queryForObject("SELECT valor_total FROM nominas WHERE id = ?", BigDecimal.class, n))
                .isEqualByComparingTo("60000");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM jornales WHERE id = ?", Integer.class, j1))
                .isZero();

        jornalUseCase.eliminarJornal(j2);
        em.flush();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nominas WHERE id = ?", Integer.class, n))
                .isZero();
    }

    @Autowired
    private com.agroruta.worker.application.ports.in.NominaUseCase nominaUseCase;

    @Test
    void borrarUnaNominaAnuladaLiberaSusJornalesYLosJornalesSeFiltranPorTrabajador() {
        long cargo = insertar(
                "INSERT INTO cargos (activo, nombre, valor_jornal, agricultor_id) VALUES (1, 'Jornalero', 60000, ?)",
                ana);
        long t1 = insertar(
                "INSERT INTO trabajadores (apellido, cedula, estado, fecha_ingreso, nombre, tipo_contrato, cargo_id, agricultor_id) "
                        + "VALUES ('Paz', '81', 'ACTIVO', ?, 'Leo', 'JORNAL', ?, ?)",
                DIA,
                cargo,
                ana);
        long t2 = insertar(
                "INSERT INTO trabajadores (apellido, cedula, estado, fecha_ingreso, nombre, tipo_contrato, cargo_id, agricultor_id) "
                        + "VALUES ('Ruiz', '82', 'ACTIVO', ?, 'Eva', 'JORNAL', ?, ?)",
                DIA,
                cargo,
                ana);
        long j1 = insertar(
                "INSERT INTO jornales (cultivo_id, fecha, liquidado, valor_jornal, trabajador_id) VALUES (?, ?, 0, 60000, ?)",
                siembra,
                DIA,
                t1);
        insertar(
                "INSERT INTO jornales (cultivo_id, fecha, liquidado, valor_jornal, trabajador_id) VALUES (?, ?, 0, 60000, ?)",
                siembra,
                DIA,
                t2);
        long n = insertar(
                "INSERT INTO nominas (estado, fecha_generacion, periodo_fin, periodo_inicio, total_jornales, valor_total, trabajador_id) "
                        + "VALUES ('ANULADA', ?, ?, ?, 1, 60000, ?)",
                DIA,
                DIA,
                DIA,
                t1);
        jdbc.update("INSERT INTO nomina_jornales (nomina_id, jornal_id) VALUES (?, ?)", n, j1);

        nominaUseCase.eliminarNomina(n);
        em.flush();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nominas WHERE id = ?", Integer.class, n))
                .isZero();
        assertThat(jornales.buscarDisponiblesParaNomina(t1, DIA, DIA))
                .extracting(j -> j.getId())
                .containsExactly(j1);

        assertThat(jornales.paginaDelEquipo(ana, null, DIA, DIA, 0, 20).total()).isEqualTo(2);
        assertThat(jornales.paginaDelEquipo(ana, t2, null, null, 0, 20).total()).isEqualTo(1);
        assertThat(jornales.resumenDelEquipo(ana, null, null, null).cantidad()).isEqualTo(2);
    }

    @Test
    void laPlanillaTraeLosJornalesDelEquipoYSabeCualesEstanEnNomina() {
        long cargo = insertar(
                "INSERT INTO cargos (activo, nombre, valor_jornal, agricultor_id) VALUES (1, 'Recolector', 50000, ?)",
                ana);
        long t = insertar(
                "INSERT INTO trabajadores (apellido, cedula, estado, fecha_ingreso, nombre, tipo_contrato, cargo_id, agricultor_id) "
                        + "VALUES ('Paz', '99', 'ACTIVO', ?, 'Leo', 'JORNAL', ?, ?)",
                DIA,
                cargo,
                ana);
        long j1 = insertar(
                "INSERT INTO jornales (cultivo_id, fecha, liquidado, valor_jornal, trabajador_id, kilos, valor_kilo) "
                        + "VALUES (?, ?, 0, 45000, ?, 50, 900)",
                siembra,
                DIA,
                t);
        long j2 = insertar(
                "INSERT INTO jornales (cultivo_id, fecha, liquidado, valor_jornal, trabajador_id) VALUES (?, ?, 0, 50000, ?)",
                siembra,
                DIA.plusDays(1),
                t);
        long n = insertar(
                "INSERT INTO nominas (estado, fecha_generacion, periodo_fin, periodo_inicio, total_jornales, valor_total, trabajador_id) "
                        + "VALUES ('PENDIENTE', ?, ?, ?, 1, 45000, ?)",
                DIA,
                DIA,
                DIA,
                t);
        jdbc.update("INSERT INTO nomina_jornales (nomina_id, jornal_id) VALUES (?, ?)", n, j1);

        var semana = jornales.buscarDelEquipo(ana, DIA, DIA.plusDays(6));
        assertThat(semana).hasSize(2);
        assertThat(semana.stream()
                        .filter(j -> j.getId() == j1)
                        .findFirst()
                        .orElseThrow()
                        .getKilos())
                .isEqualByComparingTo("50");
        assertThat(jornales.idsEnNomina(List.of(j1, j2))).containsExactly(j1);
    }
}
