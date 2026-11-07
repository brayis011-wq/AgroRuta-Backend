package com.agroruta.crop.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una temporada de cultivo en un lote. Un lote puede tener muchas siembras en el tiempo,
 * pero solo una activa (no finalizada) a la vez.
 */
@Getter
@Setter
@NoArgsConstructor
public class Siembra {

    private Long id;
    private LocalDate fechaSiembra;
    private Integer cantidadPlantas;
    /** Nombre del cultivo, p. ej. "Café" (se guarda el nombre para conservar el histórico) */
    private String tipoCultivo;
    /** Nombre de la variedad, p. ej. "Castillo" */
    private String variedad;
    /** Variedad del catálogo (null en siembras antiguas) */
    private Long variedadId;
    private EstadoCultivo estadoCultivo;
    private Long loteId;
    /** Día en que terminó la temporada (null mientras esté activa) */
    private LocalDate fechaFin;
    /**
     * Cuándo se eliminó (null si está vigente). El borrado es lógico: la siembra deja de verse
     * en el lote, pero sus actividades, fumigaciones, cosechas y jornales se conservan
     * y siguen contando en los reportes de cultivos y de costos.
     */
    private LocalDateTime eliminadoEn;

    public boolean estaEliminada() {
        return eliminadoEn != null;
    }

    /**
     * Marca la siembra como eliminada. Si seguía activa, primero cierra la temporada en
     * {@code hoy}, para que no quede "en curso" para siempre en los reportes.
     */
    public void eliminar(LocalDate hoy, LocalDateTime cuando) {
        if (estaActiva()) finalizar(hoy);
        if (eliminadoEn == null) eliminadoEn = cuando;
    }

    public Siembra(Long id, LocalDate fechaSiembra, Integer cantidadPlantas,
                   VariedadCultivo variedad, Long loteId) {
        this.id = id;
        this.fechaSiembra = fechaSiembra;
        this.cantidadPlantas = cantidadPlantas;
        cambiarVariedad(variedad);
        this.loteId = loteId;
        this.estadoCultivo = EstadoCultivo.GERMINACION;
    }

    public void cambiarVariedad(VariedadCultivo variedad) {
        if (variedad == null) return;
        this.variedadId = variedad.getId();
        this.variedad = variedad.getNombre();
        this.tipoCultivo = variedad.getTipoCultivoNombre();
    }

    public boolean estaActiva() {
        return this.estadoCultivo != EstadoCultivo.FINALIZADO;
    }

    public void avanzarEtapa() {
        switch (this.estadoCultivo) {
            case GERMINACION -> this.estadoCultivo = EstadoCultivo.CRECIMIENTO;
            case CRECIMIENTO -> this.estadoCultivo = EstadoCultivo.PRODUCCION;
            case PRODUCCION  -> this.estadoCultivo = EstadoCultivo.COSECHA;
            case COSECHA     -> this.estadoCultivo = EstadoCultivo.FINALIZADO;
            case FINALIZADO  -> throw new IllegalStateException("El cultivo ya está finalizado.");
        }
    }

    /** Devuelve la siembra a la etapa anterior (para corregir un avance por error). */
    public void retrocederEtapa() {
        switch (this.estadoCultivo) {
            case GERMINACION -> throw new IllegalStateException("El cultivo ya está en la primera etapa.");
            case CRECIMIENTO -> this.estadoCultivo = EstadoCultivo.GERMINACION;
            case PRODUCCION  -> this.estadoCultivo = EstadoCultivo.CRECIMIENTO;
            case COSECHA     -> this.estadoCultivo = EstadoCultivo.PRODUCCION;
            case FINALIZADO  -> {
                this.estadoCultivo = EstadoCultivo.COSECHA;
                this.fechaFin = null;
            }
        }
    }

    /** Cierra la temporada desde cualquier etapa (p. ej. si el cultivo se perdió). */
    public void finalizar(LocalDate fecha) {
        if (!estaActiva()) throw new IllegalStateException("El cultivo ya está finalizado.");
        this.estadoCultivo = EstadoCultivo.FINALIZADO;
        this.fechaFin = fecha;
    }

    /** "Café · Castillo" */
    public String nombreCompleto() {
        if (tipoCultivo == null || tipoCultivo.isBlank()) return variedad;
        return tipoCultivo + " · " + variedad;
    }
}
