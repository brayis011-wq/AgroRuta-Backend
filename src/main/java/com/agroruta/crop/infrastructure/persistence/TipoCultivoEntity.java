package com.agroruta.crop.infrastructure.persistence;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tipos_cultivo")
public class TipoCultivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "tipoCultivo", fetch = FetchType.EAGER)
    @OrderBy("nombre ASC")
    private List<VariedadCultivoEntity> variedades = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public List<VariedadCultivoEntity> getVariedades() { return variedades; }
}
