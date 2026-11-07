package com.agroruta.crop.domain;

import java.util.List;
import java.util.Optional;

/**
 * No hay borrado físico: eliminar una siembra es {@link Siembra#eliminar} + {@link #save},
 * así sus gastos e ingresos se conservan. findById también devuelve siembras eliminadas
 * (los reportes de costos las necesitan); las demás consultas solo ven las vigentes.
 */
public interface SiembraRepository {
    Siembra save(Siembra siembra);
    Optional<Siembra> findById(Long id);
    /** La siembra en curso del lote (no finalizada ni eliminada), si hay */
    Optional<Siembra> findActivaByLoteId(Long loteId);
    /** Las temporadas vigentes del lote, la más reciente primero */
    List<Siembra> findHistorialByLoteId(Long loteId);
    boolean existsActivaByLoteId(Long loteId);
    List<Siembra> findByEstadoCultivo(EstadoCultivo estado);
}
