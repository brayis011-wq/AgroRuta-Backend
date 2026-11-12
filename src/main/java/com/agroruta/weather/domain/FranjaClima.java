package com.agroruta.weather.domain;

import java.time.LocalDateTime;

/**
 * Tres horas del pronóstico en un punto (hora de Colombia).
 *
 * @param inicio     cuándo empieza la franja
 * @param temp       temperatura (°C)
 * @param lluviaMm   lluvia esperada en las tres horas (mm)
 * @param probLluvia probabilidad de lluvia, de 0 a 1
 * @param viento     velocidad del viento (m/s)
 */
public record FranjaClima(LocalDateTime inicio, double temp, double lluviaMm, double probLluvia, double viento) {

    /** Desde esta lluvia en tres horas el producto recién aplicado se lava */
    public static final double LLUVIA_MM = 0.5;
    public static final double PROBABILIDAD_LLUVIA = 0.6;
    /** Por encima de ~15 km/h el producto se va con el viento (deriva) */
    public static final double VIENTO_MAX = 4.2;
    public static final int HORAS = 3;

    public LocalDateTime fin() { return inicio.plusHours(HORAS); }

    public boolean llueve() { return lluviaMm >= LLUVIA_MM || probLluvia >= PROBABILIDAD_LLUVIA; }

    public boolean ventosa() { return viento > VIENTO_MAX; }

    /** Franja en la que se trabaja en el campo: empieza entre las 5 a. m. y las 4 p. m. */
    public boolean deDia() { return inicio.getHour() >= 5 && inicio.getHour() <= 16; }
}
