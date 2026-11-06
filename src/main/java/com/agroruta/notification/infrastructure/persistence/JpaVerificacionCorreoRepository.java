package com.agroruta.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JpaVerificacionCorreoRepository extends JpaRepository<VerificacionCorreoEntity, Long> {

    Optional<VerificacionCorreoEntity> findByTokenHash(String tokenHash);

    long countByUsuarioIdAndCreadoEnAfter(Long usuarioId, LocalDateTime desde);

    @Modifying
    @Query("UPDATE VerificacionCorreoEntity v SET v.usadoEn = :ahora WHERE v.usuarioId = :usuarioId AND v.usadoEn IS NULL")
    int anularPendientes(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
