package com.agroruta.finance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Junta las filas de SIPSA (un precio por producto, semana y mercado) en un precio nacional por
 * producto y semana: el promedio de los mercados, con el mínimo y el máximo que se vieron.
 */
public final class AgregadorSipsa {

    private record Clave(String producto, LocalDate semana) {}

    private static final class Suma {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal minimo;
        BigDecimal maximo;
        int mercados;
    }

    private final Map<Clave, Suma> sumas = new HashMap<>();

    /** Una fila de SIPSA; las que no tienen producto, semana o precio se ignoran */
    public void agregar(String producto, LocalDate semana, BigDecimal promedio, BigDecimal minimo, BigDecimal maximo) {
        if (producto == null || producto.isBlank() || semana == null || promedio == null || promedio.signum() <= 0)
            return;
        Suma s = sumas.computeIfAbsent(new Clave(producto.trim(), semana), k -> new Suma());
        s.total = s.total.add(promedio);
        s.mercados++;
        BigDecimal min = minimo != null && minimo.signum() > 0 ? minimo : promedio;
        BigDecimal max = maximo != null && maximo.signum() > 0 ? maximo : promedio;
        s.minimo = s.minimo == null || min.compareTo(s.minimo) < 0 ? min : s.minimo;
        s.maximo = s.maximo == null || max.compareTo(s.maximo) > 0 ? max : s.maximo;
    }

    /** Los precios de las últimas {@code semanas} semanas publicadas */
    public List<PrecioReferencia> ultimasSemanas(int semanas) {
        TreeSet<LocalDate> fechas = new TreeSet<>();
        sumas.keySet().forEach(k -> fechas.add(k.semana()));
        TreeSet<LocalDate> elegidas = new TreeSet<>();
        for (LocalDate f : fechas.descendingSet()) {
            if (elegidas.size() >= semanas) break;
            elegidas.add(f);
        }
        List<PrecioReferencia> res = new ArrayList<>();
        sumas.forEach((k, s) -> {
            if (!elegidas.contains(k.semana())) return;
            BigDecimal promedio = s.total.divide(BigDecimal.valueOf(s.mercados), 2, RoundingMode.HALF_UP);
            res.add(new PrecioReferencia(
                    k.producto(),
                    k.semana(),
                    promedio,
                    s.minimo.setScale(2, RoundingMode.HALF_UP),
                    s.maximo.setScale(2, RoundingMode.HALF_UP),
                    s.mercados));
        });
        return res;
    }

    public boolean vacio() {
        return sumas.isEmpty();
    }
}
