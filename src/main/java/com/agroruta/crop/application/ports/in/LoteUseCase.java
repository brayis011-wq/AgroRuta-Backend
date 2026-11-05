package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.EstadoLote;
import com.agroruta.crop.domain.Lote;
import java.util.List;

public interface LoteUseCase {
    Lote registrarLote(String nombre, Double area, Long fincaId);
    Lote buscarLotePorId(Long id);
    List<Lote> listarLotesPorFinca(Long fincaId);
    void eliminarLote(Long id);
    Lote actualizarGeometriaLote(Long loteId, String coordenadas,
                                 Double area, Double centroideLat, Double centroideLng);
    Lote actualizarLote(Long id, String nombre, Double area);
    /** Cambio hecho por el agricultor: solo DISPONIBLE o EN_DESCANSO y sin siembra en curso */
    Lote cambiarEstado(Long id, String estado);
    /** Uso interno: al sembrar el lote queda EN_CULTIVO */
    void ocuparLote(Long id);
    /** Uso interno: al terminar la temporada el lote queda DISPONIBLE o EN_DESCANSO */
    void liberarLote(Long id, EstadoLote estado);
}
