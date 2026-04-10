package com.bg.agroruta.cultivo.domain;

public class Lote {

    private Long id;
    private String nombre;
    private Double area;
    private EstadoLote estado;
    private Long fincaId;

    public Lote() {}

    public Lote(Long id, String nombre, Double area, Long fincaId) {
        this.id = id;
        this.nombre = nombre;
        this.area = area;
        this.fincaId = fincaId;
        this.estado = EstadoLote.DISPONIBLE;
    }

    // Lógica de negocio
    public void iniciarCultivo() {
        if (this.estado != EstadoLote.DISPONIBLE) {
            throw new IllegalStateException("El lote no está disponible para iniciar un cultivo.");
        }
        this.estado = EstadoLote.EN_CULTIVO;
    }

    public void ponerEnDescanso() {
        this.estado = EstadoLote.EN_DESCANSO;
    }

    public void disponibilizar() {
        this.estado = EstadoLote.DISPONIBLE;
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
}