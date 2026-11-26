package com.agroruta.crop.domain;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
