package com.agroruta.agriculturalInput.infrastructure.persistence;

import com.agroruta.agriculturalInput.infrastructure.persistence.AgriculturalInputEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgriculturalInputJpaRepository
        extends JpaRepository<AgriculturalInputEntity, Long> {

    List<AgriculturalInputEntity> findByActivoTrue();

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
