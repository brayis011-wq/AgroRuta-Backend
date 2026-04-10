package com.bg.agroruta.cultivo.application.ports.in;

import com.bg.agroruta.cultivo.domain.Lote;
import java.util.List;

public interface LoteUseCase {
    Lote registrarLote(Lote lote);
    Lote buscarLotePorId(Long id);
    List<Lote> listarLotesPorFinca(Long fincaId);
}