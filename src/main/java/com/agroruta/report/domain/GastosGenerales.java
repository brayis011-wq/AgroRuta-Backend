package com.agroruta.report.domain;

import java.util.List;

/** Gastos de las fincas que no son de una siembra (transporte, herramientas…), para la ganancia real. */
public record GastosGenerales(double total, List<Parte> porCategoria) {

    public record Parte(String categoria, double total) {}

    public static final GastosGenerales NINGUNO = new GastosGenerales(0, List.of());

    public GastosGenerales {
        porCategoria = porCategoria == null ? List.of() : List.copyOf(porCategoria);
    }
}
