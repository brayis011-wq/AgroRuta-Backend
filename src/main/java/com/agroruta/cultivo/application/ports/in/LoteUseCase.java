package com.agroruta.cultivo.application.ports.in;

import com.agroruta.cultivo.domain.Lote;
import java.util.List;

public interface LoteUseCase {
    Lote registrarLote(Lote lote);
    Lote buscarLotePorId(Long id);
    List<Lote> listarLotesPorFinca(Long fincaId);
}