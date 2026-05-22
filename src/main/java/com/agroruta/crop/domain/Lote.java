package com.agroruta.crop.domain;

public class Lote {

    private Long id;
    private String nombre;
    private Double area;
    private EstadoLote estado;
    private Long fincaId;
    private String coordenadas;
    private Double centroideLat;
    private Double centroideLng;

    public Lote() {}

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public EstadoLote getEstado() { return estado; }
    public void setEstado(EstadoLote estado) { this.estado = estado; }

    public Long getFincaId() { return fincaId; }
    public void setFincaId(Long fincaId) { this.fincaId = fincaId; }

    public String getCoordenadas() { return coordenadas; }
    public void setCoordenadas(String coordenadas) { this.coordenadas = coordenadas; }

    public Double getCentroideLat() { return centroideLat; }
    public void setCentroideLat(Double centroideLat) { this.centroideLat = centroideLat; }

    public Double getCentroideLng() { return centroideLng; }
    public void setCentroideLng(Double centroideLng) { this.centroideLng = centroideLng; }
}