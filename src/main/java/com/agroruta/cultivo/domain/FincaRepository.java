package com.agroruta.cultivo.domain;

import java.util.List;
import java.util.Optional;

public interface FincaRepository {
    Finca save(Finca finca);
    Optional<Finca> findById(Long id);
    List<Finca> findByAgricultorId(Long agricultorId);
    void deleteById(Long id);
}