package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.Fumigacion;

import java.util.List;
import java.util.Optional;

public interface FumigacionRepository {
    Fumigacion save(Fumigacion fumigacion);
    Optional<Fumigacion> findById(Long id);
    List<Fumigacion> findBySiembraId(Long siembraId);
    void deleteById(Long id);
    void deleteBySiembraId(Long siembraId);
}