package com.agroruta.crop.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Un cultivo del catálogo (uchuva, café, aguacate...) con sus variedades. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoCultivo {
    private Long id;
    private String nombre;
    private boolean activo = true;
    private List<VariedadCultivo> variedades = new ArrayList<>();
}
