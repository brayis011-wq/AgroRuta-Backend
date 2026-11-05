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
    /** Días entre la aplicación y la cosecha (periodo de carencia); null si no se conoce */
    private Integer diasCarencia;
    @Setter private boolean activo;       // ← Boolean → boolean
    @Setter private LocalDateTime creadoEn;

    public static AgriculturalInput create(
            String nombre,
            AgriculturalInputType tipo,
            MeasurementUnit unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras) {
        return create(nombre, tipo, unidadSugerida, dosisSugerida, reentradaHoras, null);
    }

    public static AgriculturalInput create(
            String nombre,
            AgriculturalInputType tipo,
            MeasurementUnit unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras,
            Integer diasCarencia) {

        AgriculturalInput input = new AgriculturalInput();
        input.diasCarencia   = diasCarencia;
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
        update(nombre, tipo, unidadSugerida, dosisSugerida, reentradaHoras, this.diasCarencia);
    }

    public void update(
            String nombre,
            AgriculturalInputType tipo,
            MeasurementUnit unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras,
            Integer diasCarencia) {

        this.diasCarencia    = diasCarencia;
        this.nombre          = nombre;
        this.tipo            = tipo;
        this.unidadSugerida  = unidadSugerida;
        this.dosisSugerida   = dosisSugerida;
        this.reentradaHoras  = reentradaHoras;
    }

    public void deactivate() { this.activo = false; }

    public void activate() { this.activo = true; }

    public boolean requiresReentryPeriod() {
        return this.reentradaHoras != null && this.reentradaHoras > 0;
    }
}