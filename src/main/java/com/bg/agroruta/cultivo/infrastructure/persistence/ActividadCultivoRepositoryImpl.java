package com.bg.agroruta.cultivo.infrastructure.persistence;

import com.bg.agroruta.cultivo.domain.ActividadCultivo;
import com.bg.agroruta.cultivo.domain.ActividadCultivoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ActividadCultivoRepositoryImpl implements ActividadCultivoRepository {

    private final JpaActividadCultivoRepository jpaRepository;

    public ActividadCultivoRepositoryImpl(JpaActividadCultivoRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ActividadCultivo save(ActividadCultivo actividad) {
        ActividadCultivoEntity entity = toEntity(actividad);
        ActividadCultivoEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ActividadCultivo> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ActividadCultivo> findBySiembraId(Long siembraId) {
        return jpaRepository.findBySiembraId(siembraId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private ActividadCultivoEntity toEntity(ActividadCultivo actividad) {
        ActividadCultivoEntity entity = new ActividadCultivoEntity();
        entity.setId(actividad.getId());
        entity.setTipo(actividad.getTipo());
        entity.setDescripcion(actividad.getDescripcion());
        entity.setFecha(actividad.getFecha());
        entity.setSiembraId(actividad.getSiembraId());
        return entity;
    }

    private ActividadCultivo toDomain(ActividadCultivoEntity entity) {
        ActividadCultivo actividad = new ActividadCultivo();
        actividad.setId(entity.getId());
        actividad.setTipo(entity.getTipo());
        actividad.setDescripcion(entity.getDescripcion());
        actividad.setFecha(entity.getFecha());
        actividad.setSiembraId(entity.getSiembraId());
        return actividad;
    }
}