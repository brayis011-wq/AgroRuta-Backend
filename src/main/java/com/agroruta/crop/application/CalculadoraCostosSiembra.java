package com.agroruta.crop.application;

import com.agroruta.crop.domain.*;

import com.agroruta.crop.application.ports.out.ManoObraPort.JornalDeSiembra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Cálculo de costos e ingresos de una siembra (sin base de datos, para poder probarlo). */
public final class CalculadoraCostosSiembra {

    private CalculadoraCostosSiembra() {}

    public static CostosSiembra calcular(Long siembraId, List<JornalDeSiembra> jornales,
                                         List<Fumigacion> fumigaciones, List<ActividadCultivo> actividades,
                                         List<Cosecha> cosechas) {
        TreeMap<YearMonth, double[]> meses = new TreeMap<>(); // manoObra, insumos, otros, ingresos

        double manoObra = 0;
        Map<String, double[]> porTrabajador = new LinkedHashMap<>(); // jornales, valor
        Map<String, Long> idTrabajador = new LinkedHashMap<>();
        Map<String, String> nombres = new LinkedHashMap<>();
        for (JornalDeSiembra j : jornales) {
            double v = valor(j.valor());
            manoObra += v;
            sumar(meses, j.fecha(), 0, v);
            String clave = j.trabajadorId() != null ? "id" + j.trabajadorId() : "n" + j.trabajador();
            double[] t = porTrabajador.computeIfAbsent(clave, k -> new double[2]);
            t[0] += 1;
            t[1] += v;
            idTrabajador.putIfAbsent(clave, j.trabajadorId());
            nombres.putIfAbsent(clave, j.trabajador());
        }

        double insumos = 0;
        int fumSinCosto = 0;
        for (Fumigacion f : fumigaciones) {
            if (f.getCosto() == null) { fumSinCosto++; continue; }
            insumos += f.getCosto();
            sumar(meses, f.getFecha(), 1, f.getCosto());
        }

        double otros = 0;
        for (ActividadCultivo a : actividades) {
            if (a.getCosto() == null) continue;
            otros += a.getCosto();
            sumar(meses, a.getFecha(), 2, a.getCosto());
        }

        double ingresos = 0, kgTotal = 0, kgConPrecio = 0;
        int sinPrecio = 0;
        for (Cosecha c : cosechas) {
            double kg = c.getCantidadKg() == null ? 0 : c.getCantidadKg();
            kgTotal += kg;
            if (c.getPrecioKg() == null) { sinPrecio++; continue; }
            double ingreso = kg * c.getPrecioKg();
            ingresos += ingreso;
            kgConPrecio += kg;
            sumar(meses, c.getFecha(), 3, ingreso);
        }

        double costoTotal = manoObra + insumos + otros;
        List<CostosSiembra.CostoTrabajador> trabajadores = new ArrayList<>();
        porTrabajador.forEach((clave, t) -> trabajadores.add(new CostosSiembra.CostoTrabajador(
                idTrabajador.get(clave), nombres.get(clave), (int) t[0], redondear(t[1]))));
        trabajadores.sort(Comparator.comparingDouble(CostosSiembra.CostoTrabajador::valor).reversed());

        List<CostosSiembra.CostoMes> lista = new ArrayList<>();
        meses.forEach((m, v) -> lista.add(new CostosSiembra.CostoMes(
                m.toString(), redondear(v[0]), redondear(v[1]), redondear(v[2]), redondear(v[3]))));

        return new CostosSiembra(
                siembraId, redondear(manoObra), jornales.size(), redondear(insumos), redondear(otros),
                redondear(costoTotal), redondear(ingresos), kgTotal, kgConPrecio,
                kgConPrecio > 0 ? redondear(ingresos / kgConPrecio) : null,
                kgConPrecio > 0 ? redondear(ingresos - costoTotal) : null,
                kgTotal > 0 ? redondear(costoTotal / kgTotal) : null,
                trabajadores, lista, fumSinCosto, sinPrecio);
    }

    private static void sumar(TreeMap<YearMonth, double[]> meses, LocalDate fecha, int i, double v) {
        if (fecha == null) return;
        meses.computeIfAbsent(YearMonth.from(fecha), k -> new double[4])[i] += v;
    }

    private static double valor(BigDecimal v) {
        return v == null ? 0 : v.doubleValue();
    }

    private static double redondear(double v) {
        return Math.round(v);
    }
}
