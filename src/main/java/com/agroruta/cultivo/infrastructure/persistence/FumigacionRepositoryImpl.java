package com.agroruta.cultivo.infrastructure.persistence;

import com.agroruta.cultivo.domain.Fumigacion;
import com.agroruta.cultivo.domain.FumigacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class FumigacionRepositoryImpl implements FumigacionRepository {

    private final JpaFumigacionRepository jpaRepository;

    public FumigacionRepositoryImpl(JpaFumigacionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Fumigacion save(Fumigacion fumigacion) {
        FumigacionEntity entity = toEntity(fumigacion);
        FumigacionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Fumigacion> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Fumigacion> findBySiembraId(Long siembraId) {
        return jpaRepository.findBySiembraId(siembraId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private FumigacionEntity toEntity(Fumigacion fumigacion) {
        FumigacionEntity entity = new FumigacionEntity();
        entity.setId(fumigacion.getId());
        entity.setFecha(fumigacion.getFecha());
        entity.setProducto(fumigacion.getProducto());
        entity.setDosis(fumigacion.getDosis());
        entity.setUnidadMedida(fumigacion.getUnidadMedida());
        entity.setAreaAplicada(fumigacion.getAreaAplicada());
        entity.setObservaciones(fumigacion.getObservaciones());
        entity.setSiembraId(fumigacion.getSiembraId());
        return entity;
    }

    private Fumigacion toDomain(FumigacionEntity entity) {
        Fumigacion fumigacion = new Fumigacion();
        fumigacion.setId(entity.getId());
        fumigacion.setFecha(entity.getFecha());
        fumigacion.setProducto(entity.getProducto());
        fumigacion.setDosis(entity.getDosis());
        fumigacion.setUnidadMedida(entity.getUnidadMedida());
        fumigacion.setAreaAplicada(entity.getAreaAplicada());
        fumigacion.setObservaciones(entity.getObservaciones());
        fumigacion.setSiembraId(entity.getSiembraId());
        return fumigacion;
    }
}