package com.agroruta.worker.infrastructure.persistence;


import com.agroruta.worker.domain.Actividad;
import com.agroruta.worker.domain.ActividadRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class ActividadRepositoryImpl implements ActividadRepository {

    private final JpaActividadRepository jpa;

    ActividadRepositoryImpl(JpaActividadRepository jpa) { this.jpa = jpa; }

    @Override public Actividad guardar(Actividad actividad) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(actividad)));
    }

    @Override public Optional<Actividad> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Actividad> listarTodas() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Actividad> listarActivas() {
        return jpa.findByActivaTrue().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}