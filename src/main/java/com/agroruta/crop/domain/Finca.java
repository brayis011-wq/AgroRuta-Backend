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
    /**
     * Cuándo se eliminó (null si está vigente). El borrado es lógico: la finca deja de verse,
     * pero sus lotes y siembras siguen en los reportes con sus gastos e ingresos.
     */
    private LocalDateTime eliminadoEn;

    public boolean estaEliminada() {
        return eliminadoEn != null;
    }

    public void eliminar(LocalDateTime cuando) {
        if (eliminadoEn == null) eliminadoEn = cuando;
    }

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

    public void editar(String nombre, String ubicacion, Double hectareas) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la finca es obligatorio.");
        }
        this.nombre = nombre.trim();
        this.ubicacion = ubicacion;
        this.hectareas = hectareas;
    }

    public boolean tieneCentroide() {
        return this.centroideLat != null && this.centroideLng != null;
    }
}