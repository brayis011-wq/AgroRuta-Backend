package com.agroruta.finance.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Ventas y cuentas por cobrar")
class VentaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 21);

    @Test
    void conKilosYPrecioElTotalSeCalcula() {
        Venta v = Venta.nueva(null, null, "Cooperativa", HOY, "Café pergamino", new BigDecimal("125"),
                new BigDecimal("18000"), new BigDecimal("1"), null, null);
        assertThat(v.total()).isEqualByComparingTo("2250000");
    }

    @Test
    void sinKilosNiTotalNoSeAcepta() {
        assertThatThrownBy(() -> Venta.nueva(null, null, "Juan", HOY, "Mora", null, null, null, null, null))
                .isInstanceOf(DatoInvalidoException.class);
        assertThatThrownBy(() -> Venta.nueva(null, null, " ", HOY, "Mora", null, null, BigDecimal.TEN, null, null))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void elSaldoYElEstadoDependenDeLoAbonadoYDeLaFecha() {
        Venta base = Venta.nueva(1L, null, "Juan", HOY.minusDays(20), "Mora", null, null, new BigDecimal("500000"),
                HOY.minusDays(5), null);
        Venta abonada = new Venta(1L, null, null, "Juan", base.fecha(), "Mora", null, null, base.total(), base.vence(),
                null, new BigDecimal("200000"), List.of());
        assertThat(abonada.saldo()).isEqualByComparingTo("300000");
        assertThat(abonada.estado(HOY)).isEqualTo(Venta.Estado.VENCIDA);
        assertThat(abonada.estado(HOY.minusDays(10))).isEqualTo(Venta.Estado.PENDIENTE);

        Venta pagada = new Venta(1L, null, null, "Juan", base.fecha(), "Mora", null, null, base.total(), base.vence(),
                null, new BigDecimal("500000"), List.of());
        assertThat(pagada.estado(HOY)).isEqualTo(Venta.Estado.PAGADA);
    }

    @Test
    void laFechaDePagoNoPuedeSerAnteriorALaVenta() {
        assertThatThrownBy(() -> Venta.nueva(null, null, "Juan", HOY, "Mora", null, null, BigDecimal.TEN, HOY.minusDays(1), null))
                .isInstanceOf(DatoInvalidoException.class);
    }
}
