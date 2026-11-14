package com.agroruta.report.infrastructure.adapter;

import com.agroruta.report.application.ports.out.CropQueryPort;
import com.agroruta.report.domain.CropDetail;
import com.agroruta.crop.infrastructure.persistence.FincaEntity;
import com.agroruta.crop.infrastructure.persistence.JpaCosechaRepository;
import com.agroruta.crop.infrastructure.persistence.JpaFincaRepository;
import com.agroruta.crop.infrastructure.persistence.JpaLoteRepository;
import com.agroruta.crop.infrastructure.persistence.LoteEntity;
import com.agroruta.crop.infrastructure.persistence.JpaSiembraRepository;
import com.agroruta.crop.infrastructure.persistence.SiembraEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CropQueryAdapter implements CropQueryPort {

    private final JpaSiembraRepository jpaSiembraRepository;
    private final JpaCosechaRepository jpaCosechaRepository;
    private final JpaLoteRepository jpaLoteRepository;
    private final JpaFincaRepository jpaFincaRepository;

    @Override
    public List<CropDetail> getAllCropDetails() {
        return toDetails(jpaSiembraRepository.findAll(), null);
    }

    @Override
    public List<CropDetail> getCropDetailsByAgricultor(Long agricultorId) {
        return toDetails(jpaSiembraRepository.findByAgricultorId(agricultorId), agricultorId);
    }

    /**
     * Cuatro consultas en total, sin importar cuántas siembras haya: siembras, sus lotes, sus fincas
     * y los kilos cosechados agrupados por siembra. Antes eran dos consultas más por cada siembra.
     */
    private List<CropDetail> toDetails(List<SiembraEntity> siembras, Long agricultorId) {
        if (siembras.isEmpty()) return List.of();

        Set<Long> loteIds = siembras.stream().map(SiembraEntity::getLoteId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, LoteEntity> lotes = jpaLoteRepository.findAllById(loteIds).stream()
                .collect(Collectors.toMap(LoteEntity::getId, l -> l));

        Set<Long> fincaIds = lotes.values().stream().map(LoteEntity::getFincaId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FincaEntity> fincas = jpaFincaRepository.findAllById(fincaIds).stream()
                .collect(Collectors.toMap(FincaEntity::getId, f -> f));

        // [siembraId, kg, cosechas]; la siembra sin cosechas no aparece
        Map<Long, Object[]> cosechas = new HashMap<>();
        for (Object[] fila : jpaCosechaRepository.totalesPorSiembra(agricultorId)) {
            cosechas.put(((Number) fila[0]).longValue(), fila);
        }

        return siembras.stream()
                .map(siembra -> {
                    Optional<LoteEntity> lote = Optional.ofNullable(lotes.get(siembra.getLoteId()));
                    Optional<FincaEntity> finca = lote.map(LoteEntity::getFincaId).map(fincas::get);
                    // Las eliminadas siguen aquí (borrado lógico): el historial de gastos e ingresos se conserva
                    boolean eliminada = siembra.getEliminadoEn() != null
                            || lote.map(LoteEntity::getEliminadoEn).isPresent()
                            || finca.map(FincaEntity::getEliminadoEn).isPresent();
                    Object[] cosecha = cosechas.get(siembra.getId());
                    return new CropDetail(
                            siembra.getId(),
                            nombreCultivo(siembra),
                            siembra.getEstadoCultivo().name(),
                            siembra.getFechaSiembra(),
                            ChronoUnit.DAYS.between(siembra.getFechaSiembra(), LocalDate.now()),
                            cosecha == null || cosecha[1] == null ? null : ((Number) cosecha[1]).doubleValue(),
                            cosecha == null ? 0 : ((Number) cosecha[2]).intValue(),
                            siembra.getTipoCultivo(),
                            lote.map(LoteEntity::getNombre).orElse(null),
                            finca.map(FincaEntity::getNombre).orElse(null),
                            eliminada,
                            siembra.getLoteId(),
                            lote.map(LoteEntity::getArea).orElse(null),
                            siembra.getFechaFin());
                })
                .toList();
    }

    /** "Café · Castillo"; las siembras antiguas solo tienen la variedad */
    private static String nombreCultivo(SiembraEntity s) {
        String tipo = s.getTipoCultivo();
        return (tipo == null || tipo.isBlank()) ? s.getVariedad() : tipo + " · " + s.getVariedad();
    }
}
