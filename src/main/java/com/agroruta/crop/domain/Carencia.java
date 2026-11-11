package com.agroruta.crop.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Periodo de carencia: días que deben pasar entre aplicar un producto y cosechar.
 * Sin base de datos, para poder probarlo.
 */
public final class Carencia {

    private Carencia() {}

    /**
     * La fumigación que impide cosechar en esa fecha.
     *
     * @param libreDesde primer día en que ya se puede cosechar (fecha de aplicación + días)
     */
    public record Bloqueo(String producto, LocalDate aplicado, int dias, LocalDate libreDesde) {}

    /**
     * La aplicación cuya carencia cubre la fecha de cosecha; si hay varias, la que termina más tarde.
     * Una fumigación posterior a la cosecha no cuenta, y el día en que termina la carencia ya se puede cosechar.
     */
    public static Optional<Bloqueo> queBloquea(List<Fumigacion> fumigaciones, LocalDate fechaCosecha) {
        if (fumigaciones == null || fechaCosecha == null) return Optional.empty();
        return fumigaciones.stream()
                .filter(f -> f.getFecha() != null && f.getDiasCarencia() != null && f.getDiasCarencia() > 0)
                .filter(f -> !f.getFecha().isAfter(fechaCosecha))
                .map(f -> new Bloqueo(f.getProducto(), f.getFecha(), f.getDiasCarencia(),
                        f.getFecha().plusDays(f.getDiasCarencia())))
                .filter(Objects::nonNull)
                .filter(b -> fechaCosecha.isBefore(b.libreDesde()))
                .max(Comparator.comparing(Bloqueo::libreDesde));
    }
}
