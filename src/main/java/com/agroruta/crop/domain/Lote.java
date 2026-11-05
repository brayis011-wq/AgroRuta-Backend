package com.agroruta.crop.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Lote {

    private Long id;
    private String nombre;
    private Double area;
    private EstadoLote estado;
    private Long fincaId;
    private String coordenadas;
    private Double centroideLat;
    private Double centroideLng;

    public Lote(Long id, String nombre, Double area, Long fincaId) {
        this.id = id;
        this.nombre = nombre;
        this.area = area;
        this.fincaId = fincaId;
        this.estado = EstadoLote.DISPONIBLE;
    }

    /** Se puede sembrar en un lote disponible o en descanso, no en uno ocupado. */
    public void iniciarCultivo() {
        if (this.estado == EstadoLote.EN_CULTIVO) {
            throw new IllegalStateException("El lote ya tiene un cultivo en curso.");
        }
        this.estado = EstadoLote.EN_CULTIVO;
    }

    public void editar(String nombre, Double area) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del lote es obligatorio.");
        }
        this.nombre = nombre.trim();
        this.area = area;
    }

    public void ponerEnDescanso() { this.estado = EstadoLote.EN_DESCANSO; }
    public void disponibilizar()  { this.estado = EstadoLote.DISPONIBLE; }

    public void actualizarGeometria(String coordenadas, Double area,
                                    Double centroideLat, Double centroideLng) {
        if (coordenadas == null || coordenadas.isBlank()) {
            throw new IllegalArgumentException("Las coordenadas del lote no pueden estar vacías.");
        }
        this.coordenadas  = coordenadas;
        this.area         = area;
        this.centroideLat = centroideLat;
        this.centroideLng = centroideLng;
    }

    public boolean tieneGeometria() {
        return this.coordenadas != null && !this.coordenadas.isBlank();
    }
}