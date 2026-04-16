package com.agroruta.cultivo.domain;

import java.util.List;
import java.util.Optional;

public interface CosechaRepository {
    Cosecha save(Cosecha cosecha);
    Optional<Cosecha> findById(Long id);
    List<Cosecha> findBySiembraId(Long siembraId);
    Double totalKgBySiembraId(Long siembraId);
    void deleteById(Long id);
}