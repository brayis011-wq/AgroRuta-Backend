package com.agroruta.crop.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el agricultor necesita ver de una siembra para decidir:
 * cuánto ha producido, cómo va semana a semana, si puede cosechar o entrar al lote
 * y cómo se compara con las temporadas anteriores del mismo lote.
 */
public record ResumenSiembra(
        Long siembraId,
        double totalKg,
        int cosechas,
        double kgPrimera,
        double kgSegunda,
        double kgTercera,
        /** null si no hay plantas registradas */
        Double kgPorPlanta,
        /** null si el lote no tiene área */
        Double kgPorHectarea,
        Double areaHa,
        List<SemanaCosecha> semanas,
        /** Carencia vigente (no cosechar); null si no hay */
        Restriccion carencia,
        /** Reentrada vigente (no entrar al lote); null si no hay */
        Restriccion reentrada,
        List<TemporadaComparada> otrasTemporadas
) {
    /** Kilos de la semana que empieza el lunes {@code semana} */
    public record SemanaCosecha(LocalDate semana, double primera, double segunda, double tercera) {
        public double total() { return primera + segunda + tercera; }
    }

    /**
     * @param producto     producto aplicado
     * @param aplicada     día de la aplicación
     * @param libreDesde   primer día en que ya se puede (cosechar o entrar)
     * @param diasRestantes días que faltan desde hoy
     * @param valor        días de carencia u horas de reentrada del producto
     */
    public record Restriccion(String producto, LocalDate aplicada, LocalDate libreDesde, long diasRestantes, int valor) {}

    public record TemporadaComparada(Long siembraId, String cultivo, LocalDate fechaSiembra, LocalDate fechaFin,
                                     boolean enCurso, double totalKg, Double kgPorPlanta, boolean mismoCultivo) {}
}
