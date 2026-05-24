package com.agroruta.crop.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Finca {

    private Long id;
    private String nombre;
    private String ubicacion;
    private Double hectareas;
    private Long agricultorId;
    private LocalDateTime fechaRegistro;
    private Double centroideLat;
    private Double centroideLng;

    public Finca(Long id, String nombre, String ubicacion, Double hectareas, Long agricultorId) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.hectareas = hectareas;
        this.agricultorId = agricultorId;
        this.fechaRegistro = LocalDateTime.now();
    }

    public void actualizarCentroide(Double centroideLat, Double centroideLng) {
        this.centroideLat = centroideLat;
        this.centroideLng = centroideLng;
    }

    public boolean tieneCentroide() {
        return this.centroideLat != null && this.centroideLng != null;
    }
}