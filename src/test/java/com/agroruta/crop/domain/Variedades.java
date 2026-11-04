package com.agroruta.crop.domain;

import java.util.List;

/** Variedades de uchuva del catálogo, para las pruebas */
public final class Variedades {
    public static final VariedadCultivo COLOMBIA = new VariedadCultivo(1L, "Colombia", true, 1L, "Uchuva", true);
    public static final VariedadCultivo GIGANTE  = new VariedadCultivo(2L, "Gigante",  true, 1L, "Uchuva", true);
    public static final VariedadCultivo KENYA    = new VariedadCultivo(3L, "Kenya",    true, 1L, "Uchuva", true);
    public static final List<VariedadCultivo> TODAS = List.of(COLOMBIA, GIGANTE, KENYA);

    private Variedades() {}
}
