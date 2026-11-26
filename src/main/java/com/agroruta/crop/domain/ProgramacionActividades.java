package com.agroruta.crop.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Cuándo toca una labor que se repite. La próxima vez es la última vez que se hizo más el intervalo
 * (o la fecha de inicio si todavía no se ha hecho); si ya pasó, está atrasada.
 * Sin base de datos, para poder probarlo.
 */
public final class ProgramacionActividades {

    private ProgramacionActividades() {}

    /**
     * @param proxima  fecha en que toca (puede ser anterior a hoy: atrasada); null si ya terminó
     * @param ultimaVez última vez que se hizo dentro de la programación (null si nunca)
     */
    public record Estado(LocalDate proxima, LocalDate ultimaVez, boolean atrasada, long diasAtraso) {}

    /**
     * @param hechas fechas en que se registró una labor del mismo tipo en la siembra
     */
    public static Estado estado(ActividadProgramada p, List<LocalDate> hechas, LocalDate hoy) {
        // Hacerla un poco antes de la primera fecha también cuenta
        LocalDate cuentaDesde = p.desde().minusDays(Math.max(1, p.cadaDias() / 2));
        LocalDate ultima = hechas.stream()
                .filter(Objects::nonNull)
                .filter(f -> !f.isBefore(cuentaDesde) && !f.isAfter(hoy))
                .max(LocalDate::compareTo)
                .orElse(null);
        LocalDate proxima = ultima == null ? p.desde() : ultima.plusDays(p.cadaDias());
        if (proxima.isBefore(p.desde())) proxima = p.desde();
        if (p.hasta() != null && proxima.isAfter(p.hasta())) return new Estado(null, ultima, false, 0);
        boolean atrasada = proxima.isBefore(hoy);
        return new Estado(proxima, ultima, atrasada, atrasada ? ChronoUnit.DAYS.between(proxima, hoy) : 0);
    }

    /**
     * Las próximas fechas en que toca dentro del rango. Si está atrasada, la atrasada no sale aquí (se
     * muestra aparte, hoy) y las siguientes cuentan desde hoy: se supone que se hará ahora.
     */
    public static List<LocalDate> fechas(
            ActividadProgramada p, Estado e, LocalDate desde, LocalDate hasta, LocalDate hoy) {
        List<LocalDate> res = new ArrayList<>();
        if (e.proxima() == null) return res;
        LocalDate fin = p.hasta() != null && p.hasta().isBefore(hasta) ? p.hasta() : hasta;
        LocalDate primera = e.atrasada() ? hoy.plusDays(p.cadaDias()) : e.proxima();
        for (LocalDate f = primera; !f.isAfter(fin) && res.size() < 400; f = f.plusDays(p.cadaDias())) {
            if (!f.isBefore(desde)) res.add(f);
        }
        return res;
    }
}
