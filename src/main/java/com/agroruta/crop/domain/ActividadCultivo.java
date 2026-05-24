package com.agroruta.crop.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ActividadCultivo {

    private Long id;
    private TipoActividad tipo;
    private String descripcion;
    private LocalDate fecha;
    private Long siembraId;

    public ActividadCultivo(Long id, TipoActividad tipo, String descripcion,
                            LocalDate fecha, Long siembraId) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.siembraId = siembraId;
    }
}