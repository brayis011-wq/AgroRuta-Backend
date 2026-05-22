package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.domain.FincaRepository;
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

    @Override
    public boolean existsByNombreAndAgricultorId(String nombre, Long agricultorId) {
        return jpaRepository.existsByNombreAndAgricultorId(nombre, agricultorId);
    }

    private FincaEntity toEntity(Finca finca) {
        FincaEntity entity = new FincaEntity();
        entity.setId(finca.getId());
        entity.setNombre(finca.getNombre());
        entity.setUbicacion(finca.getUbicacion());
        entity.setHectareas(finca.getHectareas());
        entity.setAgricultorId(finca.getAgricultorId());
        entity.setFechaRegistro(finca.getFechaRegistro());
        entity.setCentroideLat(finca.getCentroideLat());
        entity.setCentroideLng(finca.getCentroideLng());
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
        finca.setCentroideLat(entity.getCentroideLat());
        finca.setCentroideLng(entity.getCentroideLng());
        return finca;
    }
}