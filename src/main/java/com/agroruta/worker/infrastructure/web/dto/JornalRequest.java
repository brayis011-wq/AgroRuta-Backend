package com.agroruta.worker.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class JornalRequest {

    @NotNull(message = "El trabajador es obligatorio")
    @Positive(message = "El trabajador no es válido")
    public Long trabajadorId;

    @NotNull(message = "El cultivo es obligatorio")
    @Positive(message = "El cultivo no es válido")
    public Long cultivoId;

    // Opcional y se ignora: el backend toma el nombre del cultivo real (lote · finca).
    @Size(max = 150, message = "El nombre del cultivo no puede superar 150 caracteres")
    public String nombreCultivo;

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "No se puede registrar un jornal en fecha futura")
    public LocalDate fecha;

    @NotEmpty(message = "Selecciona al menos una actividad")
    @Size(max = 50, message = "Demasiadas actividades")
    public List<@NotNull(message = "Actividad no válida") Long> actividadIds;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    public String observaciones;
}
