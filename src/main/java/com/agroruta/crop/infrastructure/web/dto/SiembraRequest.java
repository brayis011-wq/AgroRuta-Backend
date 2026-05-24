package com.agroruta.crop.infrastructure.web.dto;
import lombok.Data;

import java.time.LocalDate;
@Data
public class SiembraRequest {
    private LocalDate fechaSiembra;
    private Integer cantidadPlantas;
    private String variedad;
    private Long loteId;

}