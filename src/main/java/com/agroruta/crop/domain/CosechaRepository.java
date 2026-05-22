package com.agroruta.crop.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CosechaRepository {
    Cosecha save(Cosecha cosecha);
    Optional<Cosecha> findById(Long id);
    List<Cosecha> findBySiembraId(Long siembraId);
    Double totalKgBySiembraId(Long siembraId);
    void deleteById(Long id);
    boolean existsByFechaAndSiembraId(LocalDate fecha, Long siembraId);  // ← nuevo
}