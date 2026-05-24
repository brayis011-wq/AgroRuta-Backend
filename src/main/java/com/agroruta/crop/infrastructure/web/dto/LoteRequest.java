package com.agroruta.crop.infrastructure.web.dto;
import lombok.Data;
@Data
public class LoteRequest {
    private String nombre;
    private Double area;
    private Long fincaId;

}