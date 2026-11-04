package com.agroruta.crop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface JpaEtapaSiembraRepository extends JpaRepository<EtapaSiembraEntity, Long> {
    List<EtapaSiembraEntity> findBySiembraIdOrderByFechaAscIdAsc(Long siembraId);
    Optional<EtapaSiembraEntity> findFirstBySiembraIdOrderByIdDesc(Long siembraId);

    @Modifying
    @Transactional
    void deleteBySiembraId(Long siembraId);
}
