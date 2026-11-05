package com.agroruta.crop.domain;

import com.agroruta.crop.domain.AlertaCultivo.Tipo;
import com.agroruta.crop.domain.CalendarioCultivo.DatosSiembra;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Decide qué contarle al agricultor hoy de una siembra: fumigaciones que tocan o están atrasadas,
 * labores programadas, y cuándo se puede volver a entrar o cosechar.
 * Sin base de datos, para poder probarlo.
 */
public final class AlertasCultivo {

    /** Una fumigación atrasada se recuerda estos días después de la fecha, no todos los días */
    static final Set<Long> DIAS_RECORDAR_ATRASO = Set.of(1L, 3L, 7L, 14L);

    private AlertasCultivo() {}

    public static List<AlertaCultivo> delDia(DatosSiembra d, String finca, LocalDate hoy) {
        List<AlertaCultivo> alertas = new ArrayList<>();
        Siembra s = d.siembra();
        if (s == null || !s.estaActiva()) return alertas;
        String lote = d.loteNombre();
        String cultivo = s.nombreCompleto();

        fumigacionPendiente(d.fumigaciones(), hoy).ifPresent(a ->
                alertas.add(new AlertaCultivo(a.tipo, finca, lote, cultivo, null, a.detalle, a.fecha)));

        for (ActividadCultivo act : d.actividades()) {
            if (act.getFecha() == null) continue;
            Tipo tipo = act.getFecha().equals(hoy) ? Tipo.LABOR_HOY
                    : act.getFecha().equals(hoy.plusDays(1)) ? Tipo.LABOR_MANANA : null;
            if (tipo == null) continue;
            String titulo = act.getTipo() == null ? "OTRO" : act.getTipo().name();
            alertas.add(new AlertaCultivo(tipo, finca, lote, cultivo, titulo, act.getDescripcion(), act.getFecha()));
        }

        for (Fumigacion f : d.fumigaciones()) {
            if (f.getFecha() == null) continue;
            if (f.getReentradaHoras() != null && f.getReentradaHoras() > 0) {
                LocalDate libre = f.getFecha().plusDays((long) Math.ceil(f.getReentradaHoras() / 24.0));
                if (libre.equals(hoy)) {
                    alertas.add(new AlertaCultivo(Tipo.REENTRADA_LIBRE, finca, lote, cultivo, f.getProducto(), null, libre));
                } else if (libre.isAfter(hoy) && !f.getFecha().isBefore(hoy.minusDays(1))) {
                    alertas.add(new AlertaCultivo(Tipo.REENTRADA_EN_CURSO, finca, lote, cultivo, f.getProducto(),
                            String.valueOf(f.getReentradaHoras()), libre));
                }
            }
            if (f.getDiasCarencia() != null && f.getDiasCarencia() > 0
                    && f.getFecha().plusDays(f.getDiasCarencia()).equals(hoy)) {
                alertas.add(new AlertaCultivo(Tipo.CARENCIA_LIBRE, finca, lote, cultivo, f.getProducto(), null, hoy));
            }
        }
        return alertas;
    }

    private record Pendiente(Tipo tipo, String detalle, LocalDate fecha) {}

    /** Usa el mismo intervalo que el calendario (mediana entre fumigaciones, o 7 días) */
    private static java.util.Optional<Pendiente> fumigacionPendiente(List<Fumigacion> fumigaciones, LocalDate hoy) {
        LocalDate ultima = fumigaciones.stream().map(Fumigacion::getFecha).filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);
        if (ultima == null || !ultima.isBefore(hoy)) return java.util.Optional.empty();
        return CalendarioCultivo.proximaFumigacion(fumigaciones, hoy).flatMap(sug -> {
            LocalDate tocaba = ultima.plusDays(sug.intervalo());
            long dias = ChronoUnit.DAYS.between(hoy, tocaba);
            if (dias == 1) return java.util.Optional.of(new Pendiente(Tipo.FUMIGACION_MANANA, null, tocaba));
            if (dias == 0) return java.util.Optional.of(new Pendiente(Tipo.FUMIGACION_HOY, null, tocaba));
            if (dias < 0 && DIAS_RECORDAR_ATRASO.contains(-dias)) {
                return java.util.Optional.of(new Pendiente(Tipo.FUMIGACION_ATRASADA, String.valueOf(-dias), tocaba));
            }
            return java.util.Optional.empty();
        });
    }
}
