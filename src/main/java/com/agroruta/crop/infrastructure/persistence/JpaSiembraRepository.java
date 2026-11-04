package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.EstadoCultivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface JpaSiembraRepository extends JpaRepository<SiembraEntity, Long> {
    Optional<SiembraEntity> findFirstByLoteIdAndEstadoCultivoNotOrderByFechaSiembraDescIdDesc(
            Long loteId, EstadoCultivo estadoCultivo);
    List<SiembraEntity> findByLoteIdOrderByFechaSiembraDescIdDesc(Long loteId);
    boolean existsByLoteIdAndEstadoCultivoNot(Long loteId, EstadoCultivo estadoCultivo);
    List<SiembraEntity> findByEstadoCultivo(EstadoCultivo estadoCultivo);

    @Query("""
        SELECT s FROM SiembraEntity s
        WHERE s.loteId IN (
            SELECT l.id FROM LoteEntity l
            WHERE l.fincaId IN (
                SELECT f.id FROM FincaEntity f WHERE f.agricultorId = :agricultorId
            )
        )
        """)
    List<SiembraEntity> findByAgricultorId(@Param("agricultorId") Long agricultorId);
}