package com.agroruta.crop.infrastructure.web.dto;
import lombok.Data;
@Data
public class FincaRequest {
    private String nombre;
    private String ubicacion;
    private Double hectareas;
    private Long agricultorId;

}