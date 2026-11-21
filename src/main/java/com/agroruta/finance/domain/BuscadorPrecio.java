package com.agroruta.finance.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encuentra el producto de SIPSA que corresponde a un cultivo y variedad del catálogo
 * ("Aguacate" + "Hass" → "Aguacate Hass"; "Mora" + "Castilla" → "Mora de Castilla").
 * Sin base de datos, para poder probarlo.
 */
public final class BuscadorPrecio {

    /** Palabras que no cuentan al comparar la variedad */
    private static final Set<String> VACIAS = Set.of("de", "del", "la", "el", "con", "y", "en");

    private BuscadorPrecio() {}

    /** Los productos cuyo nombre empieza por el cultivo, del más parecido a la variedad al menos */
    public static List<PrecioReferencia> candidatos(List<PrecioReferencia> precios, String cultivo, String variedad) {
        String base = normalizar(cultivo);
        if (base.isEmpty()) return List.of();
        Set<String> palabras = palabras(variedad);
        return precios.stream()
                .filter(p -> {
                    String n = normalizar(p.producto());
                    return n.equals(base) || n.startsWith(base + " ");
                })
                .sorted(Comparator.comparingInt((PrecioReferencia p) -> -coincidencias(p.producto(), base, palabras))
                        .thenComparingInt(p -> comun(p.producto()) ? 0 : 1)
                        .thenComparingInt(p -> p.producto().length())
                        .thenComparing(PrecioReferencia::producto))
                .toList();
    }

    public static Optional<PrecioReferencia> mejor(List<PrecioReferencia> precios, String cultivo, String variedad) {
        return candidatos(precios, cultivo, variedad).stream().findFirst();
    }

    /** Sin tildes, en minúsculas y con un solo espacio entre palabras */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static int coincidencias(String producto, String base, Set<String> palabras) {
        if (palabras.isEmpty()) return 0;
        Set<String> delProducto = palabras(normalizar(producto).substring(base.length()));
        return (int) palabras.stream().filter(delProducto::contains).count();
    }

    private static boolean comun(String producto) {
        return normalizar(producto).endsWith(" comun");
    }

    private static Set<String> palabras(String texto) {
        return Arrays.stream(normalizar(texto).split(" "))
                .filter(p -> !p.isEmpty() && !VACIAS.contains(p))
                .collect(Collectors.toSet());
    }
}
