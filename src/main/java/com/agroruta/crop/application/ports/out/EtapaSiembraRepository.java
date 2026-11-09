package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.EtapaSiembra;

import java.util.List;
import java.util.Optional;

public interface EtapaSiembraRepository {
    EtapaSiembra save(EtapaSiembra etapa);
    /** Etapas de la siembra en orden (la más antigua primero) */
    List<EtapaSiembra> findBySiembraId(Long siembraId);
    Optional<EtapaSiembra> findUltima(Long siembraId);
    void deleteById(Long id);
    void deleteBySiembraId(Long siembraId);
}
