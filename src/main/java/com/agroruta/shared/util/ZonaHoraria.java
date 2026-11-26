package com.agroruta.shared.util;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Zona horaria de la aplicación. Los agricultores están en Colombia: "hoy" y las horas que se muestran
 * se calculan siempre en la hora de Bogotá, aunque el servidor (Render) corra en UTC.
 */
public final class ZonaHoraria {

    public static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private ZonaHoraria() {}

    /** Fija la zona por defecto de la JVM, para que los {@code now()} sin reloj también usen la hora de Colombia. */
    public static void fijarPorDefecto() {
        TimeZone.setDefault(TimeZone.getTimeZone(COLOMBIA));
    }
}
