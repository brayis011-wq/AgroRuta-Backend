package com.agroruta.weather.domain;

/**
 * Lo que viene en las próximas 24 horas en un punto.
 *
 * @param tempMin   temperatura mínima (°C)
 * @param tempMax   temperatura máxima (°C)
 * @param lluviaMm  lluvia acumulada (mm)
 * @param vientoMax viento más fuerte (m/s)
 */
public record Pronostico(double tempMin, double tempMax, double lluviaMm, double vientoMax) {

    /** Por debajo de esto se pueden quemar las hojas: aviso de helada */
    public static final double TEMP_HELADA = 3.0;
    /** Lluvia fuerte en un día */
    public static final double LLUVIA_FUERTE_MM = 20.0;
    /** Con esta lluvia el producto recién aplicado se lava */
    public static final double LLUVIA_LAVA_PRODUCTO_MM = 5.0;

    public boolean riesgoHelada() { return tempMin <= TEMP_HELADA; }
    public boolean lluviaFuerte() { return lluviaMm >= LLUVIA_FUERTE_MM; }
    public boolean lluviaLavaProducto() { return lluviaMm >= LLUVIA_LAVA_PRODUCTO_MM; }
}
