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

    @Modifying
    @Query("DELETE FROM SesionEntity s WHERE s.usuarioId = :usuarioId AND s.expiraEn < :ahora")
    int borrarVencidas(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
