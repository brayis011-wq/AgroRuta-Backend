package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.application.ports.out.SiembraRepository;
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
    public boolean existsActivaByLoteId(Long loteId) {
        return jpaRepository.existsByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNull(loteId, EstadoCultivo.FINALIZADO);
    }
    @Override
    public Optional<Siembra> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Siembra> findActivaByLoteId(Long loteId) {
        return jpaRepository
                .findFirstByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNullOrderByFechaSiembraDescIdDesc(
                        loteId, EstadoCultivo.FINALIZADO)
                .map(this::toDomain);
    }

    @Override
    public List<Siembra> findHistorialByLoteId(Long loteId) {
        return jpaRepository.findByLoteIdAndEliminadoEnIsNullOrderByFechaSiembraDescIdDesc(loteId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Siembra> findByEstadoCultivo(EstadoCultivo estado) {
        return jpaRepository.findByEstadoCultivoAndEliminadoEnIsNull(estado)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    private SiembraEntity toEntity(Siembra siembra) {
        SiembraEntity entity = new SiembraEntity();
        entity.setId(siembra.getId());
        entity.setFechaSiembra(siembra.getFechaSiembra());
        entity.setCantidadPlantas(siembra.getCantidadPlantas());
        entity.setVariedad(siembra.getVariedad());
        entity.setTipoCultivo(siembra.getTipoCultivo());
        entity.setVariedadId(siembra.getVariedadId());
        entity.setEstadoCultivo(siembra.getEstadoCultivo());
        entity.setLoteId(siembra.getLoteId());
        entity.setFechaFin(siembra.getFechaFin());
        entity.setEliminadoEn(siembra.getEliminadoEn());
        return entity;
    }

    private Siembra toDomain(SiembraEntity entity) {
        Siembra siembra = new Siembra();
        siembra.setId(entity.getId());
        siembra.setFechaSiembra(entity.getFechaSiembra());
        siembra.setCantidadPlantas(entity.getCantidadPlantas());
        siembra.setVariedad(entity.getVariedad());
        siembra.setTipoCultivo(entity.getTipoCultivo());
        siembra.setVariedadId(entity.getVariedadId());
        siembra.setEstadoCultivo(entity.getEstadoCultivo());
        siembra.setLoteId(entity.getLoteId());
        siembra.setFechaFin(entity.getFechaFin());
        siembra.setEliminadoEn(entity.getEliminadoEn());
        return siembra;
    }
}