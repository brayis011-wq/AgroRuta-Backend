package com.agroruta.crop.domain;

import java.util.List;
import java.util.Optional;

public interface LoteRepository {
    Lote save(Lote lote);
    Optional<Lote> findById(Long id);
    List<Lote> findByFincaId(Long fincaId);
    boolean existsSiembraActivaEnLote(Long loteId);
    boolean existsByNombreAndFincaId(String nombre, Long fincaId);  // ← nuevo
    void deleteById(Long id);
}