package com.agroruta.cultivo.application.ports.in;

import com.agroruta.cultivo.domain.Cosecha;
import java.util.List;

public interface CosechaUseCase {
    Cosecha registrarCosecha(Cosecha cosecha);
    List<Cosecha> listarCosechasPorSiembra(Long siembraId);
    Double totalKgCosechado(Long siembraId);
}