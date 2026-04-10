package com.bg.agroruta.cultivo.application.ports.in;

import com.bg.agroruta.cultivo.domain.Siembra;
import java.util.List;

public interface SiembraUseCase {
    Siembra registrarSiembra(Siembra siembra);
    Siembra buscarSiembraPorId(Long id);
    Siembra buscarSiembraPorLote(Long loteId);
    Siembra avanzarEtapa(Long siembraId);
    List<Siembra> listarSiembrasPorEstado(String estado);
}