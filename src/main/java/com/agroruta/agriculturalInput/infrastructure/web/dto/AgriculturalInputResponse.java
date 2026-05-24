package com.agroruta.agriculturalInput.infrastructure.web.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

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
    private Boolean activo;
    private LocalDateTime creadoEn;
}