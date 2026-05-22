package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.Lote;
import java.util.List;

public interface LoteUseCase {
    Lote registrarLote(String nombre, Double area, Long fincaId);
    Lote buscarLotePorId(Long id);
    List<Lote> listarLotesPorFinca(Long fincaId);
    void eliminarLote(Long id);
    Lote actualizarGeometriaLote(Long loteId, String coordenadas,
                                 Double area, Double centroideLat, Double centroideLng);

}