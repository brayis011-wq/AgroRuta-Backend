package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.CalidadCosecha;
import com.agroruta.crop.domain.Cosecha;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CosechaRepository {
    Cosecha save(Cosecha cosecha);

    Optional<Cosecha> findById(Long id);

    List<Cosecha> findBySiembraId(Long siembraId);

    Double totalKgBySiembraId(Long siembraId);

    void deleteById(Long id);

    void deleteBySiembraId(Long siembraId);
    /** ¿Ya hay una cosecha de esa calidad ese día en la siembra? El mismo día caben primera, segunda y tercera. */
    boolean existsByFechaAndSiembraIdAndCalidad(LocalDate fecha, Long siembraId, CalidadCosecha calidad);
}
