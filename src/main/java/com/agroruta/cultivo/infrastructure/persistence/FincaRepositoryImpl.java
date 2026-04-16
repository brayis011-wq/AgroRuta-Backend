package com.agroruta.cultivo.infrastructure.persistence;

import com.agroruta.cultivo.domain.Finca;
import com.agroruta.cultivo.domain.FincaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class FincaRepositoryImpl implements FincaRepository {

    private final JpaFincaRepository jpaRepository;

    public FincaRepositoryImpl(JpaFincaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Finca save(Finca finca) {
        FincaEntity entity = toEntity(finca);
        FincaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Finca> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Finca> findByAgricultorId(Long agricultorId) {
        return jpaRepository.findByAgricultorId(agricultorId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private FincaEntity toEntity(Finca finca) {
        FincaEntity entity = new FincaEntity();
        entity.setId(finca.getId());
        entity.setNombre(finca.getNombre());
        entity.setUbicacion(finca.getUbicacion());
        entity.setHectareas(finca.getHectareas());
        entity.setAgricultorId(finca.getAgricultorId());
        entity.setFechaRegistro(finca.getFechaRegistro());
        return entity;
    }

    private Finca toDomain(FincaEntity entity) {
        Finca finca = new Finca();
        finca.setId(entity.getId());
        finca.setNombre(entity.getNombre());
        finca.setUbicacion(entity.getUbicacion());
        finca.setHectareas(entity.getHectareas());
        finca.setAgricultorId(entity.getAgricultorId());
        finca.setFechaRegistro(entity.getFechaRegistro());
        return finca;
    }
}