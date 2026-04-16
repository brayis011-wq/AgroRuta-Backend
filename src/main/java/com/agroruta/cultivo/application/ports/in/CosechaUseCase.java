package com.agroruta.cultivo.application.ports.in;

import com.agroruta.cultivo.domain.Cosecha;
import java.time.LocalDate;
import java.util.List;

public interface CosechaUseCase {
    Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg,
                             String calidad, String observaciones, Long siembraId);
    List<Cosecha> listarCosechasPorSiembra(Long siembraId);
    Double totalKgCosechado(Long siembraId);
}