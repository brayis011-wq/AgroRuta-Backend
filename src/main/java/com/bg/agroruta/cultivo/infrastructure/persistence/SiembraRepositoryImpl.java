package com.bg.agroruta.cultivo.infrastructure.persistence;

import com.bg.agroruta.cultivo.domain.EstadoCultivo;
import com.bg.agroruta.cultivo.domain.Siembra;
import com.bg.agroruta.cultivo.domain.SiembraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SiembraRepositoryImpl implements SiembraRepository {

    private final JpaSiembraRepository jpaRepository;

    public SiembraRepositoryImpl(JpaSiembraRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Siembra save(Siembra siembra) {
        SiembraEntity entity = toEntity(siembra);
        SiembraEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Siembra> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Siembra> findByLoteId(Long loteId) {
        return jpaRepository.findByLoteId(loteId).map(this::toDomain);
    }

    @Override
    public List<Siembra> findByEstadoCultivo(EstadoCultivo estado) {
        return jpaRepository.findByEstadoCultivo(estado)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private SiembraEntity toEntity(Siembra siembra) {
        SiembraEntity entity = new SiembraEntity();
        entity.setId(siembra.getId());
        entity.setFechaSiembra(siembra.getFechaSiembra());
        entity.setCantidadPlantas(siembra.getCantidadPlantas());
        entity.setVariedad(siembra.getVariedad());
        entity.setEstadoCultivo(siembra.getEstadoCultivo());
        entity.setLoteId(siembra.getLoteId());
        return entity;
    }

    private Siembra toDomain(SiembraEntity entity) {
        Siembra siembra = new Siembra();
        siembra.setId(entity.getId());
        siembra.setFechaSiembra(entity.getFechaSiembra());
        siembra.setCantidadPlantas(entity.getCantidadPlantas());
        siembra.setVariedad(entity.getVariedad());
        siembra.setEstadoCultivo(entity.getEstadoCultivo());
        siembra.setLoteId(entity.getLoteId());
        return siembra;
    }
}