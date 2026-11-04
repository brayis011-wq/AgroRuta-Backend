package com.agroruta.crop.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

/** Registro de cuándo una siembra entró en una etapa (germinación, crecimiento...). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaSiembra {

    private Long id;
    private Long siembraId;
    private EstadoCultivo etapa;
    private LocalDate fecha;
}
