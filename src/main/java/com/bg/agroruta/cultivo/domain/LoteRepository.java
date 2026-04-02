package com.bg.agroruta.cultivo.domain;

import java.util.List;
import java.util.Optional;

public interface LoteRepository {
    Lote save(Lote lote);
    Optional<Lote> findById(Long id);
    List<Lote> findByFincaId(Long fincaId);
    boolean existsSiembraActivaEnLote(Long loteId);
    void deleteById(Long id);
}