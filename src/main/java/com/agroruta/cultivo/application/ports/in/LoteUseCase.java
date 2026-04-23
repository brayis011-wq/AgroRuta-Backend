package com.agroruta.cultivo.application.ports.in;

import com.agroruta.cultivo.domain.Lote;
import java.util.List;

public interface LoteUseCase {
    Lote registrarLote(String nombre, Double area, Long fincaId);
    Lote buscarLotePorId(Long id);
    List<Lote> listarLotesPorFinca(Long fincaId);
    void eliminarLote(Long id);
}