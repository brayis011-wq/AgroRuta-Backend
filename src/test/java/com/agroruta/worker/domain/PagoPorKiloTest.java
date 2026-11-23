package com.agroruta.worker.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pago al kilo: cargo y jornal")
class PagoPorKiloTest {

    private static Trabajador con(Cargo cargo) {
        return new Trabajador(1L, "Ana", "Ruiz", "1", null, null, LocalDate.of(2026, 1, 1), TipoContrato.TEMPORAL, cargo);
    }

    private static Cargo recolector() {
        Cargo c = new Cargo(1L, "Recolector", null, null, true);
        c.fijarPago(ModalidadPago.KILO, null, new BigDecimal("900"));
        return c;
    }

    @Test
    void elCargoPorKiloGuardaElValorDelKiloYDejaElDiaEnCero() {
        Cargo c = recolector();
        assertThat(c.pagaPorKilo()).isTrue();
        assertThat(c.getValorKilo()).isEqualByComparingTo("900");
        assertThat(c.getValorJornal()).isEqualByComparingTo("0");
    }

    @Test
    void elCargoPorKiloSinValorDelKiloNoSeAcepta() {
        Cargo c = new Cargo(1L, "Recolector", null, null, true);
        assertThatThrownBy(() -> c.fijarPago(ModalidadPago.KILO, new BigDecimal("50000"), null))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void volverAPagarPorDiaBorraElValorDelKilo() {
        Cargo c = recolector();
        c.fijarPago(ModalidadPago.JORNAL, new BigDecimal("60000"), new BigDecimal("900"));
        assertThat(c.pagaPorKilo()).isFalse();
        assertThat(c.getValorKilo()).isNull();
        assertThat(c.getValorJornal()).isEqualByComparingTo("60000");
    }

    @Test
    void elJornalPorKiloValeKilosPorValorDelKilo() {
        Jornal j = new Jornal(null, LocalDate.of(2026, 11, 20), con(recolector()), 5L, "Lote 1", null);
        j.registrarKilos(new BigDecimal("62.5"));
        assertThat(j.getKilos()).isEqualByComparingTo("62.50");
        assertThat(j.getValorKilo()).isEqualByComparingTo("900");
        assertThat(j.getValorJornal()).isEqualByComparingTo("56250.00");
    }

    @Test
    void siElCargoEsPorKiloLosKilosSonObligatorios() {
        Jornal j = new Jornal(null, LocalDate.of(2026, 11, 20), con(recolector()), 5L, "Lote 1", null);
        assertThatThrownBy(() -> j.registrarKilos(null)).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void siElCargoEsPorDiaNoSeAceptanKilos() {
        Cargo dia = new Cargo(2L, "Oficios", null, new BigDecimal("60000"), true);
        Jornal j = new Jornal(null, LocalDate.of(2026, 11, 20), con(dia), 5L, "Lote 1", null);
        j.registrarKilos(null);
        assertThat(j.getValorJornal()).isEqualByComparingTo("60000");
        assertThatThrownBy(() -> j.registrarKilos(new BigDecimal("10"))).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void laNominaSumaLosJornalesPorKiloYPorDia() {
        Trabajador t = con(recolector());
        Jornal a = new Jornal(1L, LocalDate.of(2026, 11, 18), t, 5L, "Lote 1", null);
        a.registrarKilos(new BigDecimal("50"));
        Jornal b = new Jornal(2L, LocalDate.of(2026, 11, 19), t, 5L, "Lote 1", null);
        b.registrarKilos(new BigDecimal("40"));
        Nomina n = new Nomina(null, t, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 30), new java.util.ArrayList<>(java.util.List.of(a, b)));
        assertThat(n.getValorTotal()).isEqualByComparingTo("81000");
    }
}
