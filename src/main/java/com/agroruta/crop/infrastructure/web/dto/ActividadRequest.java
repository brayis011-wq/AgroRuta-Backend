package com.agroruta.crop.infrastructure.web.dto;
import lombok.Data;
import java.time.LocalDate;
@Data
public class ActividadRequest {
    private String tipo;
    private String descripcion;
    private LocalDate fecha;
    private Long siembraId;
}