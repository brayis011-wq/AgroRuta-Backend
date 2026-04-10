package com.bg.agroruta.cultivo.domain;

import java.util.List;
import java.util.Optional;

public interface SiembraRepository {
    Siembra save(Siembra siembra);
    Optional<Siembra> findById(Long id);
    Optional<Siembra> findByLoteId(Long loteId);
    List<Siembra> findByEstadoCultivo(EstadoCultivo estado);
    void deleteById(Long id);
}