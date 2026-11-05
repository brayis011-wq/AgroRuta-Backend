package com.agroruta.worker.domain;

import java.util.List;
import java.util.Optional;

public interface ActividadRepository {
    Actividad guardar(Actividad actividad);
    Optional<Actividad> buscarPorId(Long id);
    List<Actividad> listarTodas();
    List<Actividad> listarActivas();
    void eliminar(Long id);
    boolean existsByNombre(String nombre);                    // ← nuevo
    boolean existsByNombreAndIdNot(String nombre, Long id);  // ← nuevo
    /** Activas que puede usar el agricultor: las comunes y las suyas */
    List<Actividad> listarActivasPara(Long agricultorId);
    /** ¿El nombre ya existe entre las labores que ve el agricultor? (null = Administrador: solo las comunes) */
    boolean existeNombreVisible(String nombre, Long agricultorId, Long excluirId);
}