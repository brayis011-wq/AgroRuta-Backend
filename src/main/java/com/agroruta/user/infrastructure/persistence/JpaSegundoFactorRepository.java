package com.agroruta.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSegundoFactorRepository extends JpaRepository<SegundoFactorEntity, Long> {
}
