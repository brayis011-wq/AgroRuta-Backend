package com.agroruta.crop.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class Cosecha {

    private Long id;
    private LocalDate fecha;
    private Double cantidadKg;
    private CalidadCosecha calidad;
    private String observaciones;
    private Long siembraId;

    public Cosecha(Long id, LocalDate fecha, Double cantidadKg,
                   CalidadCosecha calidad, String observaciones, Long siembraId) {
        this.id = id;
        this.fecha = fecha;
        this.cantidadKg = cantidadKg;
        this.calidad = calidad;
        this.observaciones = observaciones;
        this.siembraId = siembraId;
    }
}