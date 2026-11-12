package com.agroruta.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JpaSesionRepository extends JpaRepository<SesionEntity, Long> {

    Optional<SesionEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE SesionEntity s SET s.revocadoEn = :ahora WHERE s.usuarioId = :usuarioId AND s.revocadoEn IS NULL")
    int revocarTodas(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);

    @Query("SELECT s FROM SesionEntity s WHERE s.usuarioId = :usuarioId AND s.revocadoEn IS NULL "
            + "AND s.rotadoEn IS NULL AND s.expiraEn > :ahora ORDER BY s.creadoEn DESC")
    java.util.List<SesionEntity> buscarAbiertas(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("UPDATE SesionEntity s SET s.revocadoEn = :ahora WHERE s.usuarioId = :usuarioId "
            + "AND s.familia = :familia AND s.revocadoEn IS NULL")
    int revocarFamilia(@Param("usuarioId") Long usuarioId, @Param("familia") String familia, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("UPDATE SesionEntity s SET s.revocadoEn = :ahora WHERE s.usuarioId = :usuarioId "
            + "AND (s.familia IS NULL OR s.familia <> :familia) AND s.revocadoEn IS NULL")
    int revocarOtras(@Param("usuarioId") Long usuarioId, @Param("familia") String familia, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("DELETE FROM SesionEntity s WHERE s.usuarioId = :usuarioId AND s.expiraEn < :ahora")
    int borrarVencidas(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
