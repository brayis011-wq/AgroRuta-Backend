package com.agroruta.crop.application;

import com.agroruta.crop.domain.*;

import com.agroruta.crop.application.ports.out.ManoObraPort.JornalDeSiembra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cálculo de costos e ingresos de una siembra (sin base de datos, para poder probarlo).
 * El dinero se suma en decimal, exacto, y solo el resultado se redondea a pesos enteros.
 */
public final class CalculadoraCostosSiembra {

    private static final int MANO_OBRA = 0, INSUMOS = 1, OTROS = 2, INGRESOS = 3;

    private CalculadoraCostosSiembra() {}

    public static CostosSiembra calcular(Long siembraId, List<JornalDeSiembra> jornales,
                                         List<Fumigacion> fumigaciones, List<ActividadCultivo> actividades,
                                         List<Cosecha> cosechas) {
        TreeMap<YearMonth, BigDecimal[]> meses = new TreeMap<>(); // manoObra, insumos, otros, ingresos

        BigDecimal manoObra = BigDecimal.ZERO;
        Map<String, Integer> jornalesPorTrabajador = new LinkedHashMap<>();
        Map<String, BigDecimal> valorPorTrabajador = new LinkedHashMap<>();
        Map<String, Long> idTrabajador = new LinkedHashMap<>();
        Map<String, String> nombres = new LinkedHashMap<>();
        for (JornalDeSiembra j : jornales) {
            BigDecimal v = j.valor() == null ? BigDecimal.ZERO : j.valor();
            manoObra = manoObra.add(v);
            sumar(meses, j.fecha(), MANO_OBRA, v);
            String clave = j.trabajadorId() != null ? "id" + j.trabajadorId() : "n" + j.trabajador();
            jornalesPorTrabajador.merge(clave, 1, Integer::sum);
            valorPorTrabajador.merge(clave, v, BigDecimal::add);
            idTrabajador.putIfAbsent(clave, j.trabajadorId());
            nombres.putIfAbsent(clave, j.trabajador());
        }

        BigDecimal insumos = BigDecimal.ZERO;
        int fumSinCosto = 0;
        for (Fumigacion f : fumigaciones) {
            if (f.getCosto() == null) { fumSinCosto++; continue; }
            insumos = insumos.add(f.getCosto());
            sumar(meses, f.getFecha(), INSUMOS, f.getCosto());
        }

        BigDecimal otros = BigDecimal.ZERO;
        for (ActividadCultivo a : actividades) {
            if (a.getCosto() == null) continue;
            otros = otros.add(a.getCosto());
            sumar(meses, a.getFecha(), OTROS, a.getCosto());
        }

        BigDecimal ingresos = BigDecimal.ZERO;
        double kgTotal = 0, kgConPrecio = 0;
        int sinPrecio = 0;
        for (Cosecha c : cosechas) {
            double kg = c.getCantidadKg() == null ? 0 : c.getCantidadKg();
            kgTotal += kg;
            if (c.getPrecioKg() == null) { sinPrecio++; continue; }
            BigDecimal ingreso = BigDecimal.valueOf(kg).multiply(c.getPrecioKg());
            ingresos = ingresos.add(ingreso);
            kgConPrecio += kg;
            sumar(meses, c.getFecha(), INGRESOS, ingreso);
        }

        BigDecimal costoTotal = manoObra.add(insumos).add(otros);
        List<CostosSiembra.CostoTrabajador> trabajadores = new ArrayList<>();
        valorPorTrabajador.forEach((clave, valor) -> trabajadores.add(new CostosSiembra.CostoTrabajador(
                idTrabajador.get(clave), nombres.get(clave), jornalesPorTrabajador.get(clave), pesos(valor))));
        trabajadores.sort(Comparator.comparingDouble(CostosSiembra.CostoTrabajador::valor).reversed());

        List<CostosSiembra.CostoMes> lista = new ArrayList<>();
        meses.forEach((m, v) -> lista.add(new CostosSiembra.CostoMes(
                m.toString(), pesos(v[MANO_OBRA]), pesos(v[INSUMOS]), pesos(v[OTROS]), pesos(v[INGRESOS]))));

        return new CostosSiembra(
                siembraId, pesos(manoObra), jornales.size(), pesos(insumos), pesos(otros),
                pesos(costoTotal), pesos(ingresos), kgTotal, kgConPrecio,
                kgConPrecio > 0 ? pesos(dividir(ingresos, kgConPrecio)) : null,
                kgConPrecio > 0 ? pesos(ingresos.subtract(costoTotal)) : null,
                kgTotal > 0 ? pesos(dividir(costoTotal, kgTotal)) : null,
                trabajadores, lista, fumSinCosto, sinPrecio);
    }

    private static void sumar(TreeMap<YearMonth, BigDecimal[]> meses, LocalDate fecha, int i, BigDecimal v) {
        if (fecha == null) return;
        BigDecimal[] mes = meses.computeIfAbsent(YearMonth.from(fecha), k -> {
            BigDecimal[] ceros = new BigDecimal[4];
            Arrays.fill(ceros, BigDecimal.ZERO);
            return ceros;
        });
        mes[i] = mes[i].add(v);
    }

    /** Pesos por kilo: los kilos no son dinero, así que aquí la división es aproximada */
    private static BigDecimal dividir(BigDecimal pesos, double kilos) {
        return pesos.divide(BigDecimal.valueOf(kilos), 4, RoundingMode.HALF_UP);
    }

    /** A pesos enteros, que es como se muestran */
    private static double pesos(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).doubleValue();
    }
}
