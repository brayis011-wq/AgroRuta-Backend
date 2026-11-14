package com.agroruta.shared.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DineroTest {

    @Test
    void dejaDosDecimales() {
        assertEquals(new BigDecimal("8000.00"), Dinero.redondear(new BigDecimal("8000")));
        assertEquals(new BigDecimal("1500.50"), Dinero.redondear(BigDecimal.valueOf(1500.5)));
    }

    @Test
    void medioCentavoSube() {
        assertEquals(new BigDecimal("10.13"), Dinero.redondear(new BigDecimal("10.125")));
        assertEquals(new BigDecimal("10.12"), Dinero.redondear(new BigDecimal("10.1249")));
    }

    @Test
    void sinValorSigueSinValor() {
        assertNull(Dinero.redondear(null));
    }
}
