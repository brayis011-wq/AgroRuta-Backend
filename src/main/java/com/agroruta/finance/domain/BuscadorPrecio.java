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

    /**
     * Palabras de productos que no salen de un cultivo. SIPSA también publica carnes, pollo, pescados,
     * huevos, lácteos y procesados ("Café molido", "Harina de maíz", "Fríjol enlatado", "Carne de cerdo,
     * tocino papada"): no se sugieren ni salen en el buscador.
     */
    private static final Set<String> NO_SON_CULTIVO = Set.of(
            // Procesados
            "molido",
            "molida",
            "instantaneo",
            "enlatado",
            "enlatada",
            "lata",
            "harina",
            "precocida",
            "precocido",
            "fecula",
            "cuchuco",
            "pulpa",
            "jugo",
            "mermelada",
            "polvo",
            "pasta",
            "pastas",
            "aceite",
            "galleta",
            "galletas",
            "tostado",
            "panela",
            "chocolate",
            "cafe",
            "avena",
            "gelatina",
            "mayonesa",
            "mostaza",
            "salsa",
            "sal",
            "vinagre",
            "color",
            "margarina",
            "manteca",
            // Carnes y pollo
            "carne",
            "cerdo",
            "res",
            "pollo",
            "alas",
            "muslos",
            "pechuga",
            "menudencias",
            "rabadillas",
            "pierna",
            "piernas",
            "pernil",
            // Huevos y lácteos
            "huevo",
            "queso",
            "leche",
            // Pescados y mariscos
            "pescado",
            "bagre",
            "basa",
            "blanquillo",
            "bocachico",
            "cachama",
            "capaz",
            "corvina",
            "merluza",
            "mojarra",
            "nicuro",
            "pargo",
            "robalo",
            "salmon",
            "sierra",
            "tilapia",
            "toyo",
            "trucha",
            "calamar",
            "camaron",
            "langostino",
            "almejas",
            "mariscos",
            "atun",
            "sardinas",
            "mar");

    /** Procesados solo si el nombre empieza así: "Azúcar morena" sí, "Mango de azúcar" (variedad) no */
    private static final Set<String> NO_SON_CULTIVO_AL_INICIO = Set.of("azucar", "bocadillo");

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
                .filter(p -> esCultivo(p.producto()))
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

    /** Si el producto sale de un cultivo (no es carne, pescado, huevo, lácteo ni procesado) */
    public static boolean esCultivo(String producto) {
        String primera = normalizar(producto).split(" ")[0];
        return !NO_SON_CULTIVO_AL_INICIO.contains(primera)
                && palabras(producto).stream().noneMatch(NO_SON_CULTIVO::contains);
    }

    /**
     * Buscador de productos: solo los de cultivo, y cada palabra escrita debe ser el comienzo de una palabra
     * del producto ("papa cri" → "Papa criolla limpia"; "lim tah" → "Limón Tahití"). Primero los que empiezan
     * por lo escrito, luego por orden alfabético.
     */
    public static List<PrecioReferencia> buscar(List<PrecioReferencia> precios, String texto, int maximo) {
        String q = normalizar(texto);
        if (q.length() < 2) return List.of();
        List<String> buscadas =
                Arrays.stream(q.split(" ")).filter(p -> !p.isEmpty()).toList();
        return precios.stream()
                .filter(p -> esCultivo(p.producto()))
                .filter(p -> {
                    List<String> delProducto =
                            Arrays.asList(normalizar(p.producto()).split(" "));
                    return buscadas.stream().allMatch(b -> delProducto.stream().anyMatch(w -> w.startsWith(b)));
                })
                .sorted(Comparator.comparingInt((PrecioReferencia p) -> rango(normalizar(p.producto()), q))
                        .thenComparing(p -> normalizar(p.producto())))
                .limit(maximo)
                .toList();
    }

    /** 0: empieza por lo escrito como palabra completa ("papa" → "Papa capira"); 1: lo empieza ("Papaya"); 2: el resto */
    private static int rango(String producto, String q) {
        if (producto.equals(q) || producto.startsWith(q + " ")) return 0;
        return producto.startsWith(q) ? 1 : 2;
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
