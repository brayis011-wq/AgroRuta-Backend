package com.agroruta.crop.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

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

    public Siembra(Long id, LocalDate fechaSiembra, Integer cantidadPlantas,
                   VariedadCultivo variedad, Long loteId) {
        this.id = id;
        this.fechaSiembra = fechaSiembra;
        this.cantidadPlantas = cantidadPlantas;
        if (variedad != null) {
            this.variedadId = variedad.getId();
            this.variedad = variedad.getNombre();
            this.tipoCultivo = variedad.getTipoCultivoNombre();
        }
        this.loteId = loteId;
        this.estadoCultivo = EstadoCultivo.GERMINACION;
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

    /** "Café · Castillo" */
    public String nombreCompleto() {
        if (tipoCultivo == null || tipoCultivo.isBlank()) return variedad;
        return tipoCultivo + " · " + variedad;
    }
}
