package com.agroruta.crop.domain;

import java.util.List;
import java.util.Optional;

public interface SiembraRepository {
    Siembra save(Siembra siembra);
    Optional<Siembra> findById(Long id);
    /** La siembra en curso del lote (no finalizada), si hay */
    Optional<Siembra> findActivaByLoteId(Long loteId);
    /** Todas las temporadas del lote, la más reciente primero */
    List<Siembra> findHistorialByLoteId(Long loteId);
    boolean existsActivaByLoteId(Long loteId);
    List<Siembra> findByEstadoCultivo(EstadoCultivo estado);
    void deleteById(Long id);
}
