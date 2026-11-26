package com.agroruta.crop.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Las consultas con "EliminadoEnIsNull" no ven las fincas eliminadas (borrado lógico). */
public interface JpaFincaRepository extends JpaRepository<FincaEntity, Long> {
    List<FincaEntity> findByAgricultorIdAndEliminadoEnIsNull(Long agricultorId);

    boolean existsByNombreAndAgricultorIdAndEliminadoEnIsNull(String nombre, Long agricultorId);
}
