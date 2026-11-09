package com.agroruta.agriculturalinput.infrastructure.persistence;

import com.agroruta.agriculturalinput.infrastructure.persistence.AgriculturalInputEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgriculturalInputJpaRepository
        extends JpaRepository<AgriculturalInputEntity, Long> {

    List<AgriculturalInputEntity> findByActivoTrue();

    List<AgriculturalInputEntity> findByActivoTrueOrderByNombreAsc();

    List<AgriculturalInputEntity> findAllByOrderByNombreAsc();

    @Query("""
            SELECT COUNT(a) > 0 FROM AgriculturalInputEntity a
            WHERE a.activo = true
              AND LOWER(TRIM(a.nombre)) = LOWER(:nombre)
              AND (:excluirId IS NULL OR a.id <> :excluirId)
            """)
    boolean existeActivoConNombre(@Param("nombre") String nombre, @Param("excluirId") Long excluirId);

    Optional<AgriculturalInputEntity> findByIdAndActivoTrue(Long id);

    @Query("""
            SELECT a FROM AgriculturalInputEntity a
            WHERE a.activo = true
              AND (
                LOWER(a.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(CAST(a.tipo AS string)) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY a.nombre ASC
            """)
    List<AgriculturalInputEntity> searchActiveByQuery(@Param("query") String query);
}
