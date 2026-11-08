package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaLaborRepository extends JpaRepository<LaborEntity, Long> {
    List<LaborEntity> findByActivaTrue();
    boolean existsByNombre(String nombre);                        // ← nuevo
    boolean existsByNombreAndIdNot(String nombre, Long id);      // ← para validar en actualización

    /** Labores comunes y las del agricultor, activas, por nombre */
    @Query("SELECT a FROM LaborEntity a WHERE a.activa = true AND (a.agricultorId IS NULL OR a.agricultorId = :agricultorId) ORDER BY a.nombre")
    List<LaborEntity> activasPara(@Param("agricultorId") Long agricultorId);

    /** ¿Ese nombre ya lo ve el agricultor (común o suyo)? Para el Administrador (null) solo cuentan las comunes. */
    @Query("""
            SELECT COUNT(a) > 0 FROM LaborEntity a
            WHERE LOWER(TRIM(a.nombre)) = LOWER(:nombre)
              AND (:excluirId IS NULL OR a.id <> :excluirId)
              AND (a.agricultorId IS NULL OR a.agricultorId = :agricultorId)
            """)
    boolean nombreVisible(@Param("nombre") String nombre, @Param("agricultorId") Long agricultorId,
                          @Param("excluirId") Long excluirId);
}