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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CropQueryAdapter implements CropQueryPort {

    private final JpaSiembraRepository jpaSiembraRepository;
    private final JpaCosechaRepository jpaCosechaRepository;
    private final JpaLoteRepository jpaLoteRepository;
    private final JpaFincaRepository jpaFincaRepository;

    @Override
    public List<CropDetail> getAllCropDetails() {
        return toDetails(jpaSiembraRepository.findAll());
    }

    @Override
    public List<CropDetail> getCropDetailsByAgricultor(Long agricultorId) {
        return toDetails(jpaSiembraRepository.findByAgricultorId(agricultorId));
    }

    private List<CropDetail> toDetails(List<SiembraEntity> siembras) {
        // Cache para no consultar el mismo lote o finca una vez por siembra
        Map<Long, Optional<LoteEntity>> lotes = new HashMap<>();
        Map<Long, Optional<FincaEntity>> fincas = new HashMap<>();
        return siembras.stream()
                .map(siembra -> {
                    Optional<LoteEntity> lote = siembra.getLoteId() == null ? Optional.empty()
                            : lotes.computeIfAbsent(siembra.getLoteId(), jpaLoteRepository::findById);
                    Optional<FincaEntity> finca = lote.map(LoteEntity::getFincaId)
                            .flatMap(id -> fincas.computeIfAbsent(id, jpaFincaRepository::findById));
                    // Las eliminadas siguen aquí (borrado lógico): el historial de gastos e ingresos se conserva
                    boolean eliminada = siembra.getEliminadoEn() != null
                            || lote.map(LoteEntity::getEliminadoEn).isPresent()
                            || finca.map(FincaEntity::getEliminadoEn).isPresent();
                    return new CropDetail(
                            siembra.getId(),
                            nombreCultivo(siembra),
                            siembra.getEstadoCultivo().name(),
                            siembra.getFechaSiembra(),
                            ChronoUnit.DAYS.between(siembra.getFechaSiembra(), LocalDate.now()),
                            jpaCosechaRepository.sumCantidadKgBySiembraId(siembra.getId()),
                            jpaCosechaRepository.countBySiembraId(siembra.getId()),
                            siembra.getTipoCultivo(),
                            lote.map(LoteEntity::getNombre).orElse(null),
                            finca.map(FincaEntity::getNombre).orElse(null),
                            eliminada);
                })
                .toList();
    }

    /** "Café · Castillo"; las siembras antiguas solo tienen la variedad */
    private static String nombreCultivo(SiembraEntity s) {
        String tipo = s.getTipoCultivo();
        return (tipo == null || tipo.isBlank()) ? s.getVariedad() : tipo + " · " + s.getVariedad();
    }
}
