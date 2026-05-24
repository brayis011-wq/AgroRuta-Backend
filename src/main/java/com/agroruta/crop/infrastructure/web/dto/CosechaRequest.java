package com.agroruta.crop.infrastructure.web.dto;
import lombok.Data;
import java.time.LocalDate;
@Data
public class CosechaRequest {
    private LocalDate fecha;
    private Double cantidadKg;
    private String calidad;
    private String observaciones;
    private Long siembraId;

}