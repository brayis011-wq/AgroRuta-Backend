package com.agroruta.cultivo.infrastructure.persistence;

import com.agroruta.cultivo.domain.EstadoCultivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaSiembraRepository extends JpaRepository<SiembraEntity, Long> {
    Optional<SiembraEntity> findByLoteId(Long loteId);
    List<SiembraEntity> findByEstadoCultivo(EstadoCultivo estadoCultivo);
}