package com.agroruta.worker.infrastructure.persistence;


import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.domain.CargoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class CargoRepositoryImpl implements CargoRepository {

    private final JpaCargoRepository jpa;

    CargoRepositoryImpl(JpaCargoRepository jpa) { this.jpa = jpa; }

    @Override public Cargo guardar(Cargo cargo) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(cargo)));
    }

    @Override public Optional<Cargo> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Cargo> listarTodos() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Cargo> listarActivos() {
        return jpa.findByActivoTrue().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}
