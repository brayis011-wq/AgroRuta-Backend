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

    /** Todos los insumos, activos e inactivos, ordenados por nombre (vista del Administrador). */
    List<AgriculturalInput> findAll();

    /** Busca sin importar si está activo (para reactivarlo). */
    Optional<AgriculturalInput> findAnyById(Long id);

    /** ¿Hay otro insumo activo con este nombre (sin distinguir mayúsculas)? excluirId puede ser null. */
    boolean existsActiveByNombre(String nombre, Long excluirId);
}
