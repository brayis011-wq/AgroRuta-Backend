package com.agroruta.agriculturalinput.infrastructure.web.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgriculturalInputResponse {
    private Long id;
    private String nombre;
    private String tipo;
    private String tipoDisplay;
    private String unidadSugerida;
    private Double dosisSugerida;
    private Integer reentradaHoras;
    private Integer diasCarencia;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
