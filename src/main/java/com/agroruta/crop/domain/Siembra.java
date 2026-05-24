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
    private VariedadUchuva variedad;
    private EstadoCultivo estadoCultivo;
    private Long loteId;

    public Siembra(Long id, LocalDate fechaSiembra, Integer cantidadPlantas,
                   VariedadUchuva variedad, Long loteId) {
        this.id = id;
        this.fechaSiembra = fechaSiembra;
        this.cantidadPlantas = cantidadPlantas;
        this.variedad = variedad;
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
}