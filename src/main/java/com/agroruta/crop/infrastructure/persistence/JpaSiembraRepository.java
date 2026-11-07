package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.EstadoCultivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Las consultas con "EliminadoEnIsNull" son las de uso diario (pantallas, alertas, calendario):
 * no ven las siembras eliminadas. findById y findByAgricultorId sí las ven, porque los
 * reportes de cultivos y de costos deben conservar el historial de gastos e ingresos.
 */
public interface JpaSiembraRepository extends JpaRepository<SiembraEntity, Long> {
    Optional<SiembraEntity> findFirstByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNullOrderByFechaSiembraDescIdDesc(
            Long loteId, EstadoCultivo estadoCultivo);
    List<SiembraEntity> findByLoteIdAndEliminadoEnIsNullOrderByFechaSiembraDescIdDesc(Long loteId);
    boolean existsByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNull(Long loteId, EstadoCultivo estadoCultivo);
    List<SiembraEntity> findByEstadoCultivoAndEliminadoEnIsNull(EstadoCultivo estadoCultivo);

    /** Todas las siembras del agricultor, también las eliminadas (para los reportes) */
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
