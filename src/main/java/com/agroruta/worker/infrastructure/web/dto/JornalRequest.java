package com.agroruta.worker.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

public class JornalRequest {
    public Long trabajadorId;
    public Long cultivoId;
    public String nombreCultivo;
    public LocalDate fecha;
    public List<Long> actividadIds;
    public String observaciones;
}
