package com.agroruta.crop.domain;

import java.util.List;
import java.util.Optional;

/**
 * No hay borrado físico: eliminar un lote es {@link Lote#eliminar} + {@link #save}.
 * findById también devuelve lotes eliminados (los reportes los necesitan);
 * las demás consultas solo ven los vigentes.
 */
public interface LoteRepository {
    Lote save(Lote lote);
    Optional<Lote> findById(Long id);
    /** Lotes vigentes de la finca */
    List<Lote> findByFincaId(Long fincaId);
    boolean existsSiembraActivaEnLote(Long loteId);
    /** ¿Hay un lote vigente en la finca con ese nombre? */
    boolean existsByNombreAndFincaId(String nombre, Long fincaId);
}
