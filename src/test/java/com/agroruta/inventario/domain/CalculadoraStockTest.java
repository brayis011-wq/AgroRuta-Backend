package com.agroruta.inventario.domain;

import com.agroruta.agriculturalInput.domain.MeasurementUnit;
import com.agroruta.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CalculadoraStock y UnidadBase")
class CalculadoraStockTest {

    private final LocalDate dia = LocalDate.of(2026, 11, 1);
    private final InsumoCatalogo mancozeb = new InsumoCatalogo(1L, "Mancozeb", "FUNGICIDA", MeasurementUnit.GRAMOS, true);

    private MovimientoInventario compra(double g, Double costo) {
        return MovimientoInventario.compra(9L, 1L, g, costo, dia, null, null);
    }

    @Test
    void lasComprasSumanLosUsosRestanYLosAjustesLlevanSuSigno() {
        List<MovimientoInventario> m = List.of(
                compra(2000, 40000.0),
                MovimientoInventario.uso(9L, 1L, 500, dia.plusDays(1), 7L),
                MovimientoInventario.ajuste(9L, 1L, -100, dia.plusDays(2), "se regó"));

        assertEquals(1400, CalculadoraStock.cantidad(m));
    }

    @Test
    void elCostoPromedioPonderaPorCantidadEIgnoraComprasSinPrecio() {
        List<MovimientoInventario> m = List.of(
                compra(1000, 20000.0),   // 20 $/g
                compra(3000, 120000.0),  // 40 $/g
                compra(5000, null));     // sin precio: no cuenta

        assertEquals(35.0, CalculadoraStock.costoPromedio(m), 0.0001);  // 140000 / 4000
    }

    @Test
    void sinComprasConPrecioNoHayCostoPromedio() {
        assertNull(CalculadoraStock.costoPromedio(List.of(compra(1000, null))));
    }

    @Test
    void estados() {
        assertEquals(EstadoStock.NEGATIVO, CalculadoraStock.estado(-5, null));
        assertEquals(EstadoStock.AGOTADO, CalculadoraStock.estado(0, null));
        assertEquals(EstadoStock.AGOTADO, CalculadoraStock.estado(0.0001, 100.0));
        assertEquals(EstadoStock.BAJO, CalculadoraStock.estado(100, 100.0));
        assertEquals(EstadoStock.OK, CalculadoraStock.estado(101, 100.0));
        assertEquals(EstadoStock.OK, CalculadoraStock.estado(5, null));
    }

    @Test
    void calcularArmaElResumenConValorYUltimoMovimiento() {
        StockInsumo s = CalculadoraStock.calcular(mancozeb, List.of(
                compra(2000, 40000.0),
                MovimientoInventario.uso(9L, 1L, 500, dia.plusDays(3), 7L)), 1000.0);

        assertEquals(1500, s.cantidad());
        assertEquals(20.0, s.costoPromedio(), 0.0001);
        assertEquals(30000.0, s.valorStock());
        assertEquals(EstadoStock.OK, s.estado());
        assertEquals(dia.plusDays(3), s.ultimoMovimiento());
        assertEquals(UnidadBase.GRAMOS, s.unidadBase());
    }

    @Test
    void conversionALaUnidadBase() {
        assertEquals(2500, UnidadBase.aBase(2.5, MeasurementUnit.LITROS));
        assertEquals(250, UnidadBase.aBase(250, MeasurementUnit.ML));
        assertEquals(1000, UnidadBase.aBase(1, MeasurementUnit.KG));
        assertEquals(UnidadBase.ML, UnidadBase.de(MeasurementUnit.LITROS));
        assertEquals(UnidadBase.GRAMOS, UnidadBase.de(MeasurementUnit.KG));
    }

    @Test
    void noSeMezclanLitrosConGramos() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> UnidadBase.aBaseDe(1, MeasurementUnit.LITROS, UnidadBase.GRAMOS, "Mancozeb"));
        assertTrue(e.getMessage().contains("kilos o gramos"));
    }
}
