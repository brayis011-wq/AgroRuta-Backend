package com.agroruta.worker.domain;

import java.util.List;
import java.util.Optional;

public interface LaborRepository {
    Labor guardar(Labor actividad);
    Optional<Labor> buscarPorId(Long id);
    List<Labor> listarTodas();
    List<Labor> listarActivas();
    void eliminar(Long id);
    boolean existsByNombre(String nombre);                    // ← nuevo
    boolean existsByNombreAndIdNot(String nombre, Long id);  // ← nuevo
    /** Activas que puede usar el agricultor: las comunes y las suyas */
    List<Labor> listarActivasPara(Long agricultorId);
    /** ¿El nombre ya existe entre las labores que ve el agricultor? (null = Administrador: solo las comunes) */
    boolean existeNombreVisible(String nombre, Long agricultorId, Long excluirId);
}