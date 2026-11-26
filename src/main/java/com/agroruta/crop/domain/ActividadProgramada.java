package com.agroruta.crop.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

/**
 * Una labor que se repite en una siembra: "fertilizar cada 30 días", "deshierbar cada 45".
 * No se guardan las fechas futuras: la próxima se calcula con la última vez que se hizo
 * (ver {@link ProgramacionActividades}).
 *
 * @param tipo  un {@link TipoActividad} (PODA, RIEGO…) o FERTILIZACION / FUMIGACION, que se cumplen
 *              registrando una aplicación (fumigación) del producto que corresponda
 * @param desde primera vez que toca
 * @param hasta última fecha en que puede tocar (null: mientras dure la siembra)
 */
public record ActividadProgramada(
        Long id, Long siembraId, String tipo, String descripcion, int cadaDias, LocalDate desde, LocalDate hasta) {

    public static final String FERTILIZACION = "FERTILIZACION";
    public static final String FUMIGACION = "FUMIGACION";
    static final Set<String> APLICACIONES = Set.of(FERTILIZACION, FUMIGACION);
    public static final int MAX_DIAS = 365;

    /** Revisa los datos y deja el tipo en mayúsculas */
    public static ActividadProgramada nueva(
            Long id,
            Long siembraId,
            String tipo,
            String descripcion,
            Integer cadaDias,
            LocalDate desde,
            LocalDate hasta) {
        String t = tipo == null ? "" : tipo.trim().toUpperCase(Locale.ROOT);
        if (!esTipoValido(t)) throw new DatoInvalidoException("Ese tipo de labor no existe.");
        if (cadaDias == null || cadaDias < 1 || cadaDias > MAX_DIAS) {
            throw new DatoInvalidoException("Indica cada cuántos días se repite (de 1 a " + MAX_DIAS + ").");
        }
        if (desde == null) throw new DatoInvalidoException("Indica cuándo toca la primera vez.");
        if (hasta != null && hasta.isBefore(desde)) {
            throw new DatoInvalidoException("La fecha final no puede ser anterior a la primera vez.");
        }
        String d = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        return new ActividadProgramada(id, siembraId, t, d, cadaDias, desde, hasta);
    }

    public static boolean esTipoValido(String tipo) {
        if (APLICACIONES.contains(tipo)) return true;
        for (TipoActividad t : TipoActividad.values()) {
            if (t.name().equals(tipo)) return true;
        }
        return false;
    }

    public boolean esAplicacion() {
        return APLICACIONES.contains(tipo);
    }
}
