package com.bg.agroruta.cultivo.application.ports.in;

import com.bg.agroruta.cultivo.domain.Fumigacion;
import java.util.List;

public interface FumigacionUseCase {
    Fumigacion registrarFumigacion(Fumigacion fumigacion);
    List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId);
}