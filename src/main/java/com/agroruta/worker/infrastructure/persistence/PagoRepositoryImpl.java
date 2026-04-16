package com.agroruta.worker.infrastructure.persistence;


import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.PagoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class PagoRepositoryImpl implements PagoRepository {

    private final JpaPagoRepository jpa;

    PagoRepositoryImpl(JpaPagoRepository jpa) { this.jpa = jpa; }

    @Override public Pago guardar(Pago pago) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(pago)));
    }

    @Override public Optional<Pago> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public Optional<Pago> buscarPorNomina(Long nominaId) {
        return jpa.findByNominaId(nominaId).map(WorkerMapper::toDomain);
    }

    @Override public List<Pago> buscarPorTrabajador(Long trabajadorId) {
        return jpa.findByTrabajadorId(trabajadorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Pago> listarTodos() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }
}