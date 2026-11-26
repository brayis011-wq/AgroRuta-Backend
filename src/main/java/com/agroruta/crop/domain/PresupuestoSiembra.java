package com.agroruta.crop.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lo que se espera gastar en una temporada, por las mismas partes en que se cuentan los costos
 * (mano de obra, insumos y otros). Cualquier parte puede faltar.
 *
 * @param avisado hasta qué porcentaje ya se avisó (0, 90 o 100), para no repetir el aviso cada día
 */
public record PresupuestoSiembra(
        Long siembraId, BigDecimal manoObra, BigDecimal insumos, BigDecimal otros, int avisado) {

    /** Desde aquí se avisa que se está cerca de pasarse */
    public static final int CERCA = 90;

    public static final int SUPERADO = 100;

    public enum Estado {
        BIEN,
        CERCA,
        SUPERADO
    }

    public BigDecimal total() {
        return cero(manoObra).add(cero(insumos)).add(cero(otros));
    }

    /** Presupuesto frente a lo gastado (los costos de la siembra) */
    public record Comparacion(
            PresupuestoSiembra presupuesto,
            double gastadoManoObra,
            double gastadoInsumos,
            double gastadoOtros,
            double gastadoTotal,
            double total,
            Double porcentaje,
            Estado estado,
            boolean manoObraSuperada,
            boolean insumosSuperados,
            boolean otrosSuperados) {}

    public Comparacion comparar(CostosSiembra c) {
        double mo = c == null ? 0 : c.manoObra(), in = c == null ? 0 : c.insumos(), ot = c == null ? 0 : c.otros();
        double gastado = mo + in + ot;
        double t = total().doubleValue();
        Double pct = t > 0
                ? BigDecimal.valueOf(gastado / t * 100)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue()
                : null;
        Estado e = pct == null
                ? Estado.BIEN
                : pct > SUPERADO ? Estado.SUPERADO : pct >= CERCA ? Estado.CERCA : Estado.BIEN;
        return new Comparacion(
                this,
                mo,
                in,
                ot,
                gastado,
                t,
                pct,
                e,
                superado(manoObra, mo),
                superado(insumos, in),
                superado(otros, ot));
    }

    /** El nivel que toca avisar ahora (90 o 100), o 0 si no hay nada nuevo que avisar */
    public int nivelPorAvisar(Comparacion c) {
        if (c.porcentaje() == null) return 0;
        if (c.porcentaje() > SUPERADO && avisado < SUPERADO) return SUPERADO;
        if (c.porcentaje() >= CERCA && c.porcentaje() <= SUPERADO && avisado < CERCA) return CERCA;
        return 0;
    }

    private static boolean superado(BigDecimal parte, double gastado) {
        return parte != null && parte.signum() > 0 && gastado > parte.doubleValue();
    }

    private static BigDecimal cero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
