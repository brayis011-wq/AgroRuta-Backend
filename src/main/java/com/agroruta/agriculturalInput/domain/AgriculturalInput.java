package com.agroruta.agriculturalInput.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AgriculturalInput {

    @Setter private Long id;
    private String nombre;
    private AgriculturalInputType tipo;
    private MeasurementUnit unidadSugerida;
    private Double dosisSugerida;
    private Integer reentradaHoras;
    @Setter private Boolean activo;
    @Setter private LocalDateTime creadoEn;

    public static AgriculturalInput create(
            String nombre,
            AgriculturalInputType tipo,
            MeasurementUnit unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras) {

        AgriculturalInput input = new AgriculturalInput();
        input.nombre         = nombre;
        input.tipo           = tipo;
        input.unidadSugerida = unidadSugerida;
        input.dosisSugerida  = dosisSugerida;
        input.reentradaHoras = reentradaHoras;
        input.activo         = true;
        input.creadoEn       = LocalDateTime.now();
        return input;
    }

    public void update(
            String nombre,
            AgriculturalInputType tipo,
            MeasurementUnit unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras) {

        this.nombre          = nombre;
        this.tipo            = tipo;
        this.unidadSugerida  = unidadSugerida;
        this.dosisSugerida   = dosisSugerida;
        this.reentradaHoras  = reentradaHoras;
    }

    public void deactivate() { this.activo = false; }

    public boolean requiresReentryPeriod() {
        return this.reentradaHoras != null && this.reentradaHoras > 0;
    }
}