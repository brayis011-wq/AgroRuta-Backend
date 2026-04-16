package com.agroruta.worker.infrastructure.persistence;


import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.NominaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class NominaRepositoryImpl implements NominaRepository {

    private final JpaNominaRepository jpa;

    NominaRepositoryImpl(JpaNominaRepository jpa) { this.jpa = jpa; }

    @Override public Nomina guardar(Nomina nomina) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(nomina)));
    }

    @Override public Optional<Nomina> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Nomina> buscarPorTrabajador(Long trabajadorId) {
        return jpa.findByTrabajadorId(trabajadorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Nomina> buscarPorTrabajadorYEstado(Long trabajadorId, EstadoNomina estado) {
        return jpa.findByTrabajadorIdAndEstado(trabajadorId, estado).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Nomina> buscarPorPeriodo(LocalDate inicio, LocalDate fin) {
        return jpa.findByPeriodo(inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Nomina> listarTodas() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}
