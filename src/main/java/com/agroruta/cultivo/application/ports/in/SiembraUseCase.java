package com.agroruta.cultivo.application.ports.in;

import com.agroruta.cultivo.domain.Siembra;
import java.time.LocalDate;
import java.util.List;

public interface SiembraUseCase {
    Siembra registrarSiembra(LocalDate fechaSiembra, Integer cantidadPlantas,
                             String variedad, Long loteId);
    Siembra buscarSiembraPorId(Long id);
    Siembra buscarSiembraPorLote(Long loteId);
    Siembra avanzarEtapa(Long siembraId);
    List<Siembra> listarSiembrasPorEstado(String estado);
}