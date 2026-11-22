package com.agroruta.crop.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Labores que se repiten, lluvia y presupuesto")
class ProgramacionYLluviaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 21);

    private static ActividadProgramada cada30(LocalDate desde) {
        return ActividadProgramada.nueva(1L, 9L, "fertilizacion", "Triple 15", 30, desde, null);
    }

    @Test
    void sinHacerseTocaLaPrimeraFecha() {
        var e = ProgramacionActividades.estado(cada30(HOY.plusDays(5)), List.of(), HOY);
        assertThat(e.proxima()).isEqualTo(HOY.plusDays(5));
        assertThat(e.atrasada()).isFalse();
    }

    @Test
    void laProximaEsLaUltimaVezMasElIntervalo() {
        var p = cada30(LocalDate.of(2026, 10, 1));
        var e = ProgramacionActividades.estado(p, List.of(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 11, 2)), HOY);
        assertThat(e.ultimaVez()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(e.proxima()).isEqualTo(LocalDate.of(2026, 12, 2));
    }

    @Test
    void siYaPasoLaFechaEstaAtrasada() {
        var e = ProgramacionActividades.estado(cada30(LocalDate.of(2026, 11, 1)), List.of(), HOY);
        assertThat(e.atrasada()).isTrue();
        assertThat(e.diasAtraso()).isEqualTo(20);
    }

    @Test
    void lasLaboresDeAntesDeLaProgramacionNoCuentan() {
        var e = ProgramacionActividades.estado(cada30(LocalDate.of(2026, 11, 25)), List.of(LocalDate.of(2026, 9, 1)), HOY);
        assertThat(e.ultimaVez()).isNull();
        assertThat(e.proxima()).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void lasFechasDelRangoSeRepitenHastaElFinal() {
        var p = ActividadProgramada.nueva(1L, 9L, "RIEGO", null, 7, HOY, HOY.plusDays(20));
        var e = ProgramacionActividades.estado(p, List.of(), HOY);
        assertThat(ProgramacionActividades.fechas(p, e, HOY, HOY.plusDays(60), HOY))
                .containsExactly(HOY, HOY.plusDays(7), HOY.plusDays(14));
    }

    @Test
    void siEstaAtrasadaLasSiguientesCuentanDesdeHoy() {
        var p = cada30(LocalDate.of(2026, 10, 17));
        var e = ProgramacionActividades.estado(p, List.of(), HOY);
        assertThat(e.atrasada()).isTrue();
        // No salen el 16 de noviembre (ya pasó) ni el 17 de octubre: la siguiente es hoy + 30
        assertThat(ProgramacionActividades.fechas(p, e, HOY, HOY.plusDays(60), HOY))
                .containsExactly(HOY.plusDays(30), HOY.plusDays(60));
    }

    @Test
    void elTipoDebeExistir() {
        assertThatThrownBy(() -> ActividadProgramada.nueva(null, 9L, "COSECHAR", null, 7, HOY, null))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> ActividadProgramada.nueva(null, 9L, "PODA", null, 0, HOY, null))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void laLluviaSumaLosUltimosDias() {
        var r = Lluvia.resumir(List.of(
                new Lluvia(HOY, new BigDecimal("0")),
                new Lluvia(HOY.minusDays(2), new BigDecimal("12.5")),
                new Lluvia(HOY.minusDays(10), new BigDecimal("30"))), HOY);
        assertThat(r.ultimos7()).isEqualByComparingTo("12.5");
        assertThat(r.ultimos30()).isEqualByComparingTo("42.5");
        assertThat(r.ultimaLluvia()).isEqualTo(HOY.minusDays(2));
        assertThat(r.diasSinLluvia()).isEqualTo(2);
    }

    private static CostosSiembra costos(double manoObra, double insumos, double otros) {
        return new CostosSiembra(1L, manoObra, 0, insumos, otros, manoObra + insumos + otros, 0, 0, 0,
                null, null, null, List.of(), List.of(), 0, 0);
    }

    @Test
    void elPresupuestoAvisaAl90YAlPasarse() {
        var p = new PresupuestoSiembra(1L, new BigDecimal("1000000"), new BigDecimal("500000"), null, 0);
        var bien = p.comparar(costos(600000, 200000, 0));
        assertThat(bien.estado()).isEqualTo(PresupuestoSiembra.Estado.BIEN);
        assertThat(p.nivelPorAvisar(bien)).isZero();

        var cerca = p.comparar(costos(900000, 450000, 0));
        assertThat(cerca.estado()).isEqualTo(PresupuestoSiembra.Estado.CERCA);
        assertThat(p.nivelPorAvisar(cerca)).isEqualTo(90);

        var pasado = p.comparar(costos(1100000, 450000, 20000));
        assertThat(pasado.estado()).isEqualTo(PresupuestoSiembra.Estado.SUPERADO);
        assertThat(pasado.manoObraSuperada()).isTrue();
        assertThat(pasado.insumosSuperados()).isFalse();
        assertThat(p.nivelPorAvisar(pasado)).isEqualTo(100);

        var yaAvisado = new PresupuestoSiembra(1L, new BigDecimal("1000000"), new BigDecimal("500000"), null, 100);
        assertThat(yaAvisado.nivelPorAvisar(pasado)).isZero();
    }
}
