package com.agroruta.weather.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Copia en memoria de respuestas del clima por un rato: varias pantallas y fincas vecinas piden lo mismo
 * y OpenWeather cuenta cada llamada contra la cuota gratis. Un error no se guarda (se intenta de nuevo).
 */
final class CopiaBreve<T> {

    private record Guardado<T>(long hasta, T valor) {}

    private final Duration vigencia;
    private final int maximo;
    private final Clock reloj;
    private final Map<String, Guardado<T>> copias = new ConcurrentHashMap<>();

    CopiaBreve(Duration vigencia, int maximo, Clock reloj) {
        this.vigencia = vigencia;
        this.maximo = maximo;
        this.reloj = reloj;
    }

    T obtener(String clave, Supplier<T> pedir) {
        long ahora = reloj.millis();
        Guardado<T> g = copias.get(clave);
        if (g != null && ahora < g.hasta()) return g.valor();
        T valor = pedir.get();
        if (valor != null) {
            // Sin tope la memoria crecería con cada lugar consultado; al llenarse se empieza de cero
            if (copias.size() >= maximo) copias.clear();
            copias.put(clave, new Guardado<>(ahora + vigencia.toMillis(), valor));
        }
        return valor;
    }

    /** Dos decimales ≈ 1 km: fincas vecinas comparten la misma copia */
    static String lugar(double lat, double lon) {
        return String.format(java.util.Locale.ROOT, "%.2f,%.2f", lat, lon);
    }

    static String ciudad(String nombre) {
        return nombre == null ? "" : nombre.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
