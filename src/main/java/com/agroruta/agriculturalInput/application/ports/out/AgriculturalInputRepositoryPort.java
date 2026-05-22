package com.agroruta.agriculturalInput.application.ports.out;

import com.agroruta.agriculturalInput.domain.AgriculturalInput;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (driven port).
 * La capa de aplicación define el contrato; la infraestructura lo implementa.
 * La dependencia apunta hacia adentro: infraestructura → aplicación.
 */
public interface AgriculturalInputRepositoryPort {

    List<AgriculturalInput> findAllActive();

    List<AgriculturalInput> searchByQuery(String query);

    Optional<AgriculturalInput> findById(Long id);

    AgriculturalInput save(AgriculturalInput input);

    boolean existsById(Long id);
}
