package com.agroruta.inventory.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Cálculo del stock de un insumo a partir de sus movimientos (sin base de datos, para poder probarlo). */
public final class CalculadoraStock {

    /** Por debajo de esto (en ml o g) se considera cero: evita "quedan 0,0000001 ml" por redondeos */
    static final double CASI_CERO = 0.0005;

    private CalculadoraStock() {}

    public static double cantidad(List<MovimientoInventario> movimientos) {
        return redondear(movimientos.stream().mapToDouble(MovimientoInventario::efecto).sum());
    }

    /**
     * Costo de cada ml o g según las compras con precio (promedio ponderado por cantidad).
     * Null si ninguna compra tiene precio.
     */
    public static Double costoPromedio(List<MovimientoInventario> movimientos) {
        double costo = 0, cantidad = 0;
        for (MovimientoInventario m : movimientos) {
            if (m.getTipo() != TipoMovimiento.COMPRA || m.getCostoTotal() == null || m.getCantidad() <= 0) continue;
            costo += m.getCostoTotal();
            cantidad += m.getCantidad();
        }
        return cantidad > 0 ? costo / cantidad : null;
    }

    public static EstadoStock estado(double cantidad, Double minimo) {
        if (cantidad < -CASI_CERO) return EstadoStock.NEGATIVO;
        if (cantidad <= CASI_CERO) return EstadoStock.AGOTADO;
        if (minimo != null && cantidad <= minimo) return EstadoStock.BAJO;
        return EstadoStock.OK;
    }

    public static StockInsumo calcular(InsumoCatalogo insumo, List<MovimientoInventario> movimientos, Double minimo) {
        double cantidad = cantidad(movimientos);
        Double promedio = costoPromedio(movimientos);
        Double valor = promedio == null ? null : Math.round(Math.max(cantidad, 0) * promedio * 100) / 100.0;
        LocalDate ultimo = movimientos.stream().map(MovimientoInventario::getFecha)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        return new StockInsumo(insumo.id(), insumo.nombre(), insumo.tipo(), insumo.unidadBase(),
                cantidad, minimo, promedio, valor, estado(cantidad, minimo), ultimo);
    }

    private static double redondear(double v) {
        double r = Math.round(v * 1000) / 1000.0;
        return Math.abs(r) <= CASI_CERO ? 0 : r;
    }
}
