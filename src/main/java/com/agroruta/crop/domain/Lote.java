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

    public void iniciarCultivo() {
        if (this.estado != EstadoLote.DISPONIBLE) {
            throw new IllegalStateException("El lote no está disponible para iniciar un cultivo.");
        }
        this.estado = EstadoLote.EN_CULTIVO;
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