package com.agroruta.notification.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaRestablecimientoRepository extends JpaRepository<RestablecimientoEntity, Long> {

    Optional<RestablecimientoEntity> findByTokenHash(String tokenHash);

    long countByUsuarioIdAndCreadoEnAfter(Long usuarioId, LocalDateTime desde);

    @Modifying
    @Query(
            "UPDATE RestablecimientoEntity r SET r.usadoEn = :ahora WHERE r.usuarioId = :usuarioId AND r.usadoEn IS NULL")
    int anularPendientes(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
