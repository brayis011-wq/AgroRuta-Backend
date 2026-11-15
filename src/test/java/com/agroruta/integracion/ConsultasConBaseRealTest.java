package com.agroruta.integracion;

import com.agroruta.crop.infrastructure.persistence.JpaCosechaRepository;
import com.agroruta.shared.security.ConsultaPropiedad;
import com.agroruta.shared.security.Recurso;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.infrastructure.persistence.JpaPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las consultas escritas a mano (JPQL y SQL) y las reglas únicas, contra MySQL de verdad.
 * Cada prueba corre en una transacción que se deshace al terminar.
 */
@Transactional
class ConsultasConBaseRealTest extends ConBaseReal {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);

    @Autowired private ConsultaPropiedad consultaPropiedad;
    @Autowired private JpaCosechaRepository cosechas;
    @Autowired private JpaPagoRepository pagos;
    @Autowired private TrabajadorRepository trabajadores;

    private long ana;    // agricultora dueña de todo
    private long beto;   // otro agricultor, sin nada
    private long siembra;
    private long cargo;
    private long trabajador;
    private final Map<Recurso, Long> ids = new EnumMap<>(Recurso.class);

    @BeforeEach
    void datos() {
        ana = usuario("ana");
        beto = usuario("beto");

        long finca = insertar("INSERT INTO fincas (agricultor_id, fecha_registro, hectareas, nombre, ubicacion) VALUES (?, ?, 3, 'El Triunfo', 'Subia')",
                ana, LocalDateTime.now());
        long lote = insertar("INSERT INTO lotes (area, estado, finca_id, nombre) VALUES (1.5, 'EN_CULTIVO', ?, 'Lote A')", finca);
        siembra = insertar("INSERT INTO siembras (cantidad_plantas, estado_cultivo, fecha_siembra, lote_id, variedad) VALUES (100, 'PRODUCCION', ?, ?, 'Bola roja')",
                DIA.minusDays(60), lote);
        long cosecha = cosecha(DIA, "PRIMERA", 12.5);
        long fumigacion = insertar("INSERT INTO fumigaciones (area_aplicada, dosis, fecha, producto, siembra_id, unidad_medida) VALUES (1, 2, ?, 'Mancozeb', ?, 'KG')",
                DIA.minusDays(20), siembra);
        long actividad = insertar("INSERT INTO actividades_cultivo (descripcion, fecha, siembra_id, tipo) VALUES ('Riego', ?, ?, 'RIEGO')", DIA, siembra);

        long labor = insertar("INSERT INTO actividades (activa, nombre, agricultor_id) VALUES (1, 'Recolectar', ?)", ana);
        cargo = insertar("INSERT INTO cargos (activo, nombre, valor_jornal, agricultor_id) VALUES (1, 'Recolector', 50000, ?)", ana);
        trabajador = trabajador("1.052.334-120", ana);
        long jornal = insertar("INSERT INTO jornales (cultivo_id, fecha, liquidado, valor_jornal, trabajador_id) VALUES (?, ?, 0, 50000, ?)",
                siembra, DIA, trabajador);
        long nomina = nomina("PAGADA");
        long pago = pago(nomina, DIA, "100000.00");

        ids.put(Recurso.FINCA, finca);
        ids.put(Recurso.LOTE, lote);
        ids.put(Recurso.SIEMBRA, siembra);
        ids.put(Recurso.COSECHA, cosecha);
        ids.put(Recurso.FUMIGACION, fumigacion);
        ids.put(Recurso.ACTIVIDAD_CULTIVO, actividad);
        ids.put(Recurso.LABOR, labor);
        ids.put(Recurso.TRABAJADOR, trabajador);
        ids.put(Recurso.CARGO, cargo);
        ids.put(Recurso.JORNAL, jornal);
        ids.put(Recurso.NOMINA, nomina);
        ids.put(Recurso.PAGO, pago);
    }

    // ── propiedad ───────────────────────────────────────────────────────────

    @Test
    void cadaRecursoEsDeSuDuenoYDeNadieMas() {
        for (Recurso recurso : Recurso.values()) {
            Long id = ids.get(recurso);
            assertTrue(consultaPropiedad.esDueno(recurso, id, ana), recurso + " es de Ana");
            assertFalse(consultaPropiedad.esDueno(recurso, id, beto), recurso + " no es de Beto");
            assertFalse(consultaPropiedad.esDueno(recurso, 999_999_999L, ana), recurso + " que no existe no es de nadie");
        }
    }

    // ── reportes ────────────────────────────────────────────────────────────

    @Test
    void totalesPorSiembraSumaKilosYCuentaCosechas() {
        cosecha(DIA, "SEGUNDA", 7.5);
        cosecha(DIA.plusDays(1), "PRIMERA", 5);

        List<Object[]> deAna = cosechas.totalesPorSiembra(ana);

        assertEquals(1, deAna.size());
        assertEquals(siembra, ((Number) deAna.get(0)[0]).longValue());
        assertEquals(25.0, ((Number) deAna.get(0)[1]).doubleValue(), 0.0001);
        assertEquals(3L, ((Number) deAna.get(0)[2]).longValue());
        assertTrue(cosechas.totalesPorSiembra(beto).isEmpty(), "Beto no tiene siembras");
        assertTrue(cosechas.totalesPorSiembra(null).stream().anyMatch(f -> ((Number) f[0]).longValue() == siembra),
                "sin agricultor (Administrador) salen todas");
    }

    @Test
    void totalesPorMesAgrupaLosPagosDelAgricultor() {
        pago(nomina("PAGADA"), DIA.plusDays(2), "50000.50");
        pago(nomina("PAGADA"), DIA.minusMonths(1), "30000.00");

        List<Object[]> meses = pagos.totalesPorMes(ana, DIA.minusMonths(1).withDayOfMonth(1));

        assertEquals(2, meses.size(), "octubre y noviembre");
        Object[] noviembre = meses.stream().filter(f -> ((Number) f[1]).intValue() == 11).findFirst().orElseThrow();
        assertEquals(2026, ((Number) noviembre[0]).intValue());
        assertEquals(0, new BigDecimal("150000.50").compareTo((BigDecimal) noviembre[2]));
        assertEquals(2L, ((Number) noviembre[3]).longValue());
        assertEquals(1, pagos.totalesPorMes(ana, DIA.withDayOfMonth(1)).size(), "desde noviembre, solo noviembre");
        assertTrue(pagos.totalesPorMes(beto, DIA.minusMonths(1)).isEmpty(), "Beto no ha pagado nada");
    }

    // ── cédula ──────────────────────────────────────────────────────────────

    @Test
    void laMismaCedulaPuedeEstarConDosAgricultoresPeroNoDosVecesConUno() {
        long cargoDeBeto = insertar("INSERT INTO cargos (activo, nombre, valor_jornal, agricultor_id) VALUES (1, 'Recolector', 60000, ?)", beto);
        insertar("INSERT INTO trabajadores (apellido, cedula, estado, fecha_ingreso, nombre, tipo_contrato, cargo_id, agricultor_id) VALUES ('Herrera', '1.052.334-120', 'ACTIVO', ?, 'Luis', 'JORNAL', ?, ?)",
                DIA, cargoDeBeto, beto);

        List<Trabajador> conEsaCedula = trabajadores.buscarPorCedulaNormalizada("1052334120");
        assertEquals(2, conEsaCedula.size(), "la búsqueda ignora puntos y guiones y trae los dos registros");
        assertTrue(trabajadores.existePorCedulaYAgricultor("1.052.334-120", ana));
        assertFalse(trabajadores.existePorCedulaYAgricultor("1.052.334-120", 999_999_999L));

        assertThrows(DuplicateKeyException.class, () -> trabajador("1.052.334-120", ana));
    }

    // ── cosechas ────────────────────────────────────────────────────────────

    @Test
    void elMismoDiaCabeUnaCosechaPorCalidad() {
        cosecha(DIA, "SEGUNDA", 7.5);
        cosecha(DIA, "TERCERA", 2);

        assertTrue(cosechas.existsByFechaAndSiembraIdAndCalidad(DIA, siembra, com.agroruta.crop.domain.CalidadCosecha.SEGUNDA));
        assertThrows(DuplicateKeyException.class, () -> cosecha(DIA, "PRIMERA", 1));
    }

    @Test
    void unaCosechaNoPuedeApuntarAUnaSiembraQueNoExiste() {
        assertThrows(DataIntegrityViolationException.class, () ->
                insertar("INSERT INTO cosechas (calidad, cantidad_kg, fecha, siembra_id) VALUES ('PRIMERA', 1, ?, 999999999)", DIA));
    }

    @Test
    void elPrecioSeGuardaConDosDecimales() {
        long id = insertar("INSERT INTO cosechas (calidad, cantidad_kg, fecha, siembra_id, precio_kg) VALUES ('TERCERA', 1, ?, ?, 2500.555)",
                DIA.plusDays(5), siembra);

        assertEquals(new BigDecimal("2500.56"), cosechas.findById(id).orElseThrow().getPrecioKg());
    }

    // ── apoyo ───────────────────────────────────────────────────────────────

    private long usuario(String nombre) {
        return insertar("INSERT INTO usuarios (activo, email, fecha_creacion, nombre, password, rol) VALUES (1, ?, ?, ?, 'sin-clave', 'Agricultor')",
                nombre + "-" + System.nanoTime() + "@agroruta.test", LocalDateTime.now(), nombre);
    }

    private long cosecha(LocalDate fecha, String calidad, double kg) {
        return insertar("INSERT INTO cosechas (calidad, cantidad_kg, fecha, siembra_id) VALUES (?, ?, ?, ?)", calidad, kg, fecha, siembra);
    }

    private long trabajador(String cedula, long agricultor) {
        return insertar("INSERT INTO trabajadores (apellido, cedula, estado, fecha_ingreso, nombre, tipo_contrato, cargo_id, agricultor_id) VALUES ('Herrera', ?, 'ACTIVO', ?, 'Luis', 'JORNAL', ?, ?)",
                cedula, DIA, cargo, agricultor);
    }

    private long nomina(String estado) {
        return insertar("INSERT INTO nominas (estado, fecha_generacion, periodo_fin, periodo_inicio, total_jornales, valor_total, trabajador_id) VALUES (?, ?, ?, ?, 2, 100000, ?)",
                estado, DIA, DIA, DIA.minusDays(7), trabajador);
    }

    private long pago(long nomina, LocalDate fecha, String monto) {
        return insertar("INSERT INTO pagos (fecha_pago, metodo_pago, monto, nomina_id) VALUES (?, 'EFECTIVO', ?, ?)",
                fecha, new BigDecimal(monto), nomina);
    }
}
