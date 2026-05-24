package com.agroruta.crop.infrastructure.web.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FumigacionRequest {
    private LocalDate fecha;
    private String producto;
    private Long agriculturalInputId;
    private Double dosis;
    private String unidadMedida;
    private Double areaAplicada;
    private String observaciones;
    private Long siembraId;
}