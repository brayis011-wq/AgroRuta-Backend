package com.agroruta.integracion;

import com.agroruta.crop.application.CostosCalendarioService;
import com.agroruta.crop.application.ports.in.CosechaUseCase;
import com.agroruta.crop.domain.Cosecha;
import com.agroruta.crop.domain.Recolector;
import com.agroruta.finance.application.ports.in.VentasUseCase;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.EstadoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La cosecha con quién la recogió (sus jornales y la nómina), la bodega de kilos y los ingresos por ventas. */
@Transactional
class CosechaConRecolectoresConBaseRealTest extends ConBaseReal {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);

    @Autowired private CosechaUseCase cosechas;
    @Autowired private VentasUseCase ventas;
    @Autowired private CostosCalendarioService costos;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    private long ana;
    private long siembra;
    private long leo;   // cargo por kilo
    private long eva;   // cargo por día
    private long nominaEva;

    @BeforeEach
    void datos() {
        ana = insertar("INSERT INTO usuarios (activo, email, fecha_creacion, nombre, password, rol) VALUES (1, ?, ?, 'Ana', 'x', 'Agricultor')",
                "ana-" + System.nanoTime() + "@agroruta.test", LocalDateTime.now());
        long finca = insertar("INSERT INTO fincas (agricultor_id, fecha_registro, hectareas, nombre, ubicacion) VALUES (?, ?, 3, 'La Esperanza', 'Subia')",
                ana, LocalDateTime.now());
        long lote = insertar("INSERT INTO lotes (area, estado, finca_id, nombre) VALUES (1.5, 'EN_CULTIVO', ?, 'Lote 1')", finca);
        siembra = insertar("INSERT INTO siembras (cantidad_plantas, estado_cultivo, fecha_siembra, lote_id, variedad, tipo_cultivo) "
                + "VALUES (100, 'PRODUCCION', ?, ?, 'Castillo', 'Café')", DIA.minusDays(90), lote);
        long alKilo = insertar("INSERT INTO cargos (activo, nombre, valor_jornal, agricultor_id, modalidad_pago, valor_kilo) "
                + "VALUES (1, 'Recolector', 0, ?, 'KILO', 900)", ana);
        long alDia = insertar("INSERT INTO cargos (activo, nombre, valor_jornal, agricultor_id) VALUES (1, 'Jornalero', 60000, ?)", ana);
        leo = insertar("INSERT INTO trabajadores (apellido, cedula, estado, fecha_ingreso, nombre, tipo_contrato, cargo_id, agricultor_id) "
                + "VALUES ('Paz', '501', 'ACTIVO', ?, 'Leo', 'JORNAL', ?, ?)", DIA.minusDays(30), alKilo, ana);
        eva = insertar("INSERT INTO trabajadores (apellido, cedula, estado, fecha_ingreso, nombre, tipo_contrato, cargo_id, agricultor_id) "
                + "VALUES ('Ruiz', '502', 'ACTIVO', ?, 'Eva', 'JORNAL', ?, ?)", DIA.minusDays(30), alDia, ana);
        // Eva ya tiene la nómina pendiente de la semana con un día de antes
        long antes = insertar("INSERT INTO jornales (cultivo_id, fecha, liquidado, valor_jornal, trabajador_id) VALUES (?, ?, 0, 60000, ?)",
                siembra, DIA.minusDays(1), eva);
        nominaEva = insertar("INSERT INTO nominas (estado, fecha_generacion, periodo_fin, periodo_inicio, total_jornales, valor_total, trabajador_id) "
                + "VALUES ('PENDIENTE', ?, ?, ?, 1, 60000, ?)", DIA, DIA.plusDays(4), DIA.minusDays(2), eva);
        jdbc.update("INSERT INTO nomina_jornales (nomina_id, jornal_id) VALUES (?, ?)", nominaEva, antes);
    }

    private static Recolector porKilo(long trabajador, String kilos) {
        return new Recolector(trabajador, "KILO", new BigDecimal(kilos), null, null);
    }

    private static Recolector porDia(long trabajador) {
        return new Recolector(trabajador, "DIA", null, null, null);
    }

    private BigDecimal valorDe(long trabajador, long cosecha) {
        return jdbc.queryForObject("SELECT valor_jornal FROM jornales WHERE trabajador_id = ? AND cosecha_id = ?",
                BigDecimal.class, trabajador, cosecha);
    }

    @Test
    void losRecolectoresQuedanComoJornalesYEntranALaNominaPendiente() {
        Cosecha c = cosechas.registrarCosecha(DIA, 120.0, "PRIMERA", null, siembra, null, false,
                List.of(porKilo(leo, "70"), porDia(eva)));
        em.flush();

        assertThat(c.getRecolectores()).hasSize(2);
        assertThat(valorDe(leo, c.getId())).isEqualByComparingTo("63000");   // 70 kg × 900
        assertThat(valorDe(eva, c.getId())).isEqualByComparingTo("60000");   // el día del cargo
        assertThat(jdbc.queryForObject("SELECT valor_total FROM nominas WHERE id = ?", BigDecimal.class, nominaEva))
                .isEqualByComparingTo("120000");
        Integer conLabor = jdbc.queryForObject("SELECT COUNT(*) FROM jornal_actividades ja JOIN actividades a ON a.id = ja.actividad_id "
                + "JOIN jornales j ON j.id = ja.jornal_id WHERE j.cosecha_id = ? AND a.nombre = 'Recolección'", Integer.class, c.getId());
        assertThat(conLabor).isEqualTo(2);

        // Corregir los kilos de Leo y el valor del día de Eva: la nómina pendiente se recalcula
        cosechas.actualizarCosecha(c.getId(), DIA, 120.0, "PRIMERA", null, null, false,
                List.of(porKilo(leo, "80"), new Recolector(eva, "DIA", null, null, new BigDecimal("65000"))));
        em.flush();
        assertThat(valorDe(leo, c.getId())).isEqualByComparingTo("72000");
        assertThat(jdbc.queryForObject("SELECT valor_total FROM nominas WHERE id = ?", BigDecimal.class, nominaEva))
                .isEqualByComparingTo("125000");

        // Borrar la cosecha quita sus jornales y la nómina vuelve a como estaba
        cosechas.eliminarCosecha(c.getId());
        em.flush();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM jornales WHERE cosecha_id = ?", Integer.class, c.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT valor_total FROM nominas WHERE id = ?", BigDecimal.class, nominaEva))
                .isEqualByComparingTo("60000");
    }

    @Test
    void noSeAceptanMasKilosQueLosCosechadosNiRepetirAlguien() {
        assertThatThrownBy(() -> cosechas.registrarCosecha(DIA, 50.0, "PRIMERA", null, siembra, null, false,
                List.of(porKilo(leo, "70"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("más de lo cosechado");
        assertThatThrownBy(() -> cosechas.registrarCosecha(DIA, 150.0, "SEGUNDA", null, siembra, null, false,
                List.of(porKilo(leo, "70"), porKilo(leo, "10"))))
                .hasMessageContaining("dos veces");
    }

    @Test
    void unJornalDeUnaNominaAprobadaNoSeCambiaDesdeLaCosecha() {
        Cosecha c = cosechas.registrarCosecha(DIA, 100.0, "PRIMERA", null, siembra, null, false, List.of(porKilo(leo, "60")));
        em.flush();
        long jornal = jdbc.queryForObject("SELECT id FROM jornales WHERE cosecha_id = ?", Long.class, c.getId());
        long aprobada = insertar("INSERT INTO nominas (estado, fecha_generacion, periodo_fin, periodo_inicio, total_jornales, valor_total, trabajador_id) "
                + "VALUES ('APROBADA', ?, ?, ?, 1, 54000, ?)", DIA, DIA, DIA, leo);
        jdbc.update("INSERT INTO nomina_jornales (nomina_id, jornal_id) VALUES (?, ?)", aprobada, jornal);

        assertThat(cosechas.buscarCosechaPorId(c.getId()).getRecolectores()).singleElement()
                .satisfies(r -> assertThat(r.editable()).isFalse());
        assertThatThrownBy(() -> cosechas.actualizarCosecha(c.getId(), DIA, 100.0, "PRIMERA", null, null, false,
                List.of(porKilo(leo, "65"))))
                .isInstanceOf(EstadoInvalidoException.class).hasMessageContaining("aprobada");
        // Igual, sin cambiar a Leo, se puede corregir el resto de la cosecha
        cosechas.actualizarCosecha(c.getId(), DIA, 110.0, "PRIMERA", "Corregida", null, false, List.of(porKilo(leo, "60")));
        em.flush();

        // Al borrar la cosecha, el jornal aprobado se queda (el trabajo se hizo) pero ya no apunta a ella
        cosechas.eliminarCosecha(c.getId());
        em.flush();
        assertThat(jdbc.queryForObject("SELECT cosecha_id FROM jornales WHERE id = ?", Long.class, jornal)).isNull();
    }

    @Test
    void laBodegaDeKilosRestaLoVendidoYLosIngresosSalenDeLasVentas() {
        cosechas.registrarCosecha(DIA, 120.0, "PRIMERA", null, siembra, new BigDecimal("9000"), false, null);
        em.flush();
        // Sin ventas, los ingresos son el valor estimado de la cosecha (como antes)
        var sinVentas = costos.costos(siembra);
        assertThat(sinVentas.fuenteIngresos()).isEqualTo("COSECHAS");
        assertThat(sinVentas.ingresos()).isEqualTo(1_080_000);

        ventas.registrar(ana, new VentasUseCase.DatosVenta(siembra, "Cooperativa", DIA.plusDays(1), "Café Castillo",
                new BigDecimal("50"), new BigDecimal("12000"), null, null, null), true, "EFECTIVO");
        em.flush();

        var bodega = ventas.kilosEnBodega(ana);
        assertThat(bodega).singleElement().satisfies(k -> {
            assertThat(k.kgCosechados()).isEqualByComparingTo("120");
            assertThat(k.kgVendidos()).isEqualByComparingTo("50");
            assertThat(k.kgDisponibles()).isEqualByComparingTo("70");
        });
        var conVentas = costos.costos(siembra);
        assertThat(conVentas.fuenteIngresos()).isEqualTo("VENTAS");
        assertThat(conVentas.ingresos()).isEqualTo(600_000);
        assertThat(conVentas.kgVendidos()).isEqualTo(50);
    }
    @Test
    void noSeVendeMasDeLoCosechadoNiSeBorraLaCosechaYaVendida() {
        Cosecha c = cosechas.registrarCosecha(DIA, 80.0, "PRIMERA", null, siembra, null, false, null);
        em.flush();
        var datos = new VentasUseCase.DatosVenta(siembra, "Cooperativa", DIA, "Café", new BigDecimal("90"), new BigDecimal("10000"), null, null, null);
        assertThatThrownBy(() -> ventas.registrar(ana, datos, false, null))
                .isInstanceOf(com.agroruta.shared.exception.DatoInvalidoException.class).hasMessageContaining("solo quedan 80 kg");
        var sinKilos = new VentasUseCase.DatosVenta(siembra, "Cooperativa", DIA, "Café", null, null, new BigDecimal("500000"), null, null);
        assertThatThrownBy(() -> ventas.registrar(ana, sinKilos, false, null)).hasMessageContaining("cuántos kilos");

        var venta = ventas.registrar(ana, new VentasUseCase.DatosVenta(siembra, "Cooperativa", DIA, "Café",
                new BigDecimal("60"), new BigDecimal("10000"), null, null, null), false, null);
        em.flush();
        // Al editar cuentan los kilos que ya tenía: 60 + 20 que quedan
        ventas.actualizar(venta.id(), new VentasUseCase.DatosVenta(siembra, "Cooperativa", DIA, "Café",
                new BigDecimal("80"), new BigDecimal("10000"), null, null, null));
        em.flush();
        assertThatThrownBy(() -> ventas.actualizar(venta.id(), new VentasUseCase.DatosVenta(siembra, "Cooperativa", DIA, "Café",
                new BigDecimal("81"), new BigDecimal("10000"), null, null, null))).hasMessageContaining("solo quedan 80 kg");

        // La cosecha ya vendida no se baja ni se borra
        assertThatThrownBy(() -> cosechas.actualizarCosecha(c.getId(), DIA, 70.0, "PRIMERA", null, null, false, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Ya vendiste 80 kg");
        assertThatThrownBy(() -> cosechas.eliminarCosecha(c.getId())).hasMessageContaining("Ya vendiste 80 kg");
        cosechas.actualizarCosecha(c.getId(), DIA, 95.0, "PRIMERA", null, null, false, null);
    }

    @Test
    void lasVentasDeAntesQuePasanDeLoCosechadoSeVenEnLaBodega() {
        // Venta anotada antes de que existiera la cosecha (como se podía antes)
        jdbc.update("INSERT INTO ventas (agricultor_id, siembra_id, comprador, fecha, producto, cantidad_kg, precio_kg, total, creado_en) "
                + "VALUES (?, ?, 'Cooperativa', ?, 'Café', 150, 4000, 600000, NOW())", ana, siembra, DIA);
        cosechas.registrarCosecha(DIA, 75.0, "PRIMERA", null, siembra, null, false, null);
        em.flush();
        assertThat(ventas.kilosEnBodega(ana)).singleElement().satisfies(k -> {
            assertThat(k.kgDisponibles()).isEqualByComparingTo("0");
            assertThat(k.kgSinCosecha()).isEqualByComparingTo("75");
        });
    }
}
