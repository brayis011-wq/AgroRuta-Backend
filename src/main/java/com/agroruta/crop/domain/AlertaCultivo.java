package com.agroruta.crop.domain;

import java.time.LocalDate;

/**
 * Algo del cultivo que el agricultor debería saber hoy (para el correo diario).
 *
 * @param tipo    ver {@link Tipo}
 * @param titulo  producto o labor
 * @param detalle días de atraso, fecha hasta la que no se puede entrar, etc. (puede ser null)
 */
public record AlertaCultivo(Tipo tipo, String finca, String lote, String cultivo,
                            String titulo, String detalle, LocalDate fecha) {

    public enum Tipo {
        /** Ya pasó la fecha habitual de fumigar y no se ha registrado */
        FUMIGACION_ATRASADA,
        FUMIGACION_HOY,
        FUMIGACION_MANANA,
        LABOR_HOY,
        LABOR_MANANA,
        /** Se fumigó hace poco: todavía no se puede entrar al lote */
        REENTRADA_EN_CURSO,
        /** Desde hoy ya se puede entrar al lote */
        REENTRADA_LIBRE,
        /** Desde hoy ya se puede cosechar (terminó la carencia) */
        CARENCIA_LIBRE
    }

    public boolean esFumigacionPendiente() {
        return tipo == Tipo.FUMIGACION_ATRASADA || tipo == Tipo.FUMIGACION_HOY || tipo == Tipo.FUMIGACION_MANANA;
    }
}
