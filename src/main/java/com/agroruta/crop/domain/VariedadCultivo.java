package com.agroruta.crop.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Variedad de un cultivo del catálogo (p. ej. Castillo para café). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VariedadCultivo {
    private Long id;
    private String nombre;
    private boolean activo = true;
    private Long tipoCultivoId;
    private String tipoCultivoNombre;
    /** Solo se puede sembrar si la variedad y su cultivo están activos */
    private boolean tipoCultivoActivo = true;
}
