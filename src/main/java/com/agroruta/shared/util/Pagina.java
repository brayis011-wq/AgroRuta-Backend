package com.agroruta.shared.util;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados. Sirve en el dominio y en la API sin depender de Spring Data.
 *
 * @param contenido    los elementos de esta página
 * @param pagina       número de página, desde 0
 * @param tamano       elementos por página
 * @param total        elementos en total (todas las páginas)
 * @param totalPaginas páginas en total
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long total, int totalPaginas) {

    /** Tamaño por defecto y máximo para las listas paginadas. */
    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    public <R> Pagina<R> map(Function<T, R> f) {
        return new Pagina<>(contenido.stream().map(f).toList(), pagina, tamano, total, totalPaginas);
    }

    /** Normaliza el número de página que llega en la URL (nunca negativo). */
    public static int numero(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    /** Normaliza el tamaño que llega en la URL (entre 1 y TAMANO_MAXIMO). */
    public static int tamano(Integer size) {
        if (size == null || size < 1) return TAMANO_POR_DEFECTO;
        return Math.min(size, TAMANO_MAXIMO);
    }
}
