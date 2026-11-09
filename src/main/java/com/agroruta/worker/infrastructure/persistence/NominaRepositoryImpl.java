package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.ResumenNominas;
import java.math.BigDecimal;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import com.agroruta.shared.util.Pagina;

import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.application.ports.out.NominaRepository;
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
    @Override public List<Nomina> listarPorAgricultor(Long agricultorId) {
        return jpa.findByAgricultorId(agricultorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Pagina<Nomina> pagina(Long agricultorId, Long trabajadorId, EstadoNomina estado, int pagina, int tamano) {
        var orden = Sort.by(Sort.Order.desc("fechaGeneracion"), Sort.Order.desc("id"));
        return aPagina(jpa.pagina(agricultorId, trabajadorId, estado, PageRequest.of(pagina, tamano, orden)),
                WorkerMapper::toDomain);
    }

    @Override
    public List<ResumenNominas> resumenPorEstado(Long agricultorId, Long trabajadorId) {
        return jpa.resumenPorEstado(agricultorId, trabajadorId).stream()
                .map(f -> new ResumenNominas((EstadoNomina) f[0], ((Number) f[1]).longValue(), decimal(f[2])))
                .toList();
    }

    private static <E, D> Pagina<D> aPagina(Page<E> p, java.util.function.Function<E, D> f) {
        return new Pagina<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }

    private static BigDecimal decimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }
}
