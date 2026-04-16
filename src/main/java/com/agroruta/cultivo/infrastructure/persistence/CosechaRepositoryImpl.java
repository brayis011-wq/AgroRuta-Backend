package com.agroruta.cultivo.infrastructure.persistence;

import com.agroruta.cultivo.domain.Cosecha;
import com.agroruta.cultivo.domain.CosechaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CosechaRepositoryImpl implements CosechaRepository {

    private final JpaCosechaRepository jpaRepository;

    public CosechaRepositoryImpl(JpaCosechaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Cosecha save(Cosecha cosecha) {
        CosechaEntity entity = toEntity(cosecha);
        CosechaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Cosecha> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Cosecha> findBySiembraId(Long siembraId) {
        return jpaRepository.findBySiembraId(siembraId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Double totalKgBySiembraId(Long siembraId) {
        return jpaRepository.sumCantidadKgBySiembraId(siembraId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private CosechaEntity toEntity(Cosecha cosecha) {
        CosechaEntity entity = new CosechaEntity();
        entity.setId(cosecha.getId());
        entity.setFecha(cosecha.getFecha());
        entity.setCantidadKg(cosecha.getCantidadKg());
        entity.setCalidad(cosecha.getCalidad());
        entity.setObservaciones(cosecha.getObservaciones());
        entity.setSiembraId(cosecha.getSiembraId());
        return entity;
    }

    private Cosecha toDomain(CosechaEntity entity) {
        Cosecha cosecha = new Cosecha();
        cosecha.setId(entity.getId());
        cosecha.setFecha(entity.getFecha());
        cosecha.setCantidadKg(entity.getCantidadKg());
        cosecha.setCalidad(entity.getCalidad());
        cosecha.setObservaciones(entity.getObservaciones());
        cosecha.setSiembraId(entity.getSiembraId());
        return cosecha;
    }
}