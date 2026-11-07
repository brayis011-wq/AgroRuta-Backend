package com.agroruta.crop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Las consultas con "EliminadoEnIsNull" no ven los lotes eliminados (borrado lógico). */
public interface JpaLoteRepository extends JpaRepository<LoteEntity, Long> {
    List<LoteEntity> findByFincaIdAndEliminadoEnIsNull(Long fincaId);
    boolean existsByNombreAndFincaIdAndEliminadoEnIsNull(String nombre, Long fincaId);
}
