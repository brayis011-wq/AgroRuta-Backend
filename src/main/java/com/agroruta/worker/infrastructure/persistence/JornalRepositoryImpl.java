package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.ResumenJornales;
import java.math.BigDecimal;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import com.agroruta.shared.util.Pagina;

import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.application.ports.out.JornalRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class JornalRepositoryImpl implements JornalRepository {

    private final JpaJornalRepository jpa;

    JornalRepositoryImpl(JpaJornalRepository jpa) { this.jpa = jpa; }

    @Override public Jornal guardar(Jornal jornal) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(jornal)));
    }

    @Override public Optional<Jornal> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Jornal> buscarPorTrabajador(Long trabajadorId) {
        return jpa.findByTrabajadorId(trabajadorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jpa.findByTrabajadorIdAndFechaBetween(trabajadorId, inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarPorCultivo(Long cultivoId) {
        return jpa.findByCultivoId(cultivoId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarPorCultivoYPeriodo(Long cultivoId, LocalDate inicio, LocalDate fin) {
        return jpa.findByCultivoIdAndFechaBetween(cultivoId, inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarNoLiquidadosPorTrabajador(Long trabajadorId) {
        return jpa.findByTrabajadorIdAndLiquidadoFalse(trabajadorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarNoLiquidadosPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jpa.findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(trabajadorId, inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }
    @Override
    public List<Jornal> buscarDisponiblesParaNomina(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jpa.findDisponiblesParaNomina(trabajadorId, inicio, fin)
                .stream()
                .map(WorkerMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }

    @Override
    public Pagina<Jornal> paginaPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano) {
        var orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
        return aPagina(jpa.paginaPorTrabajador(trabajadorId, inicio, fin, PageRequest.of(pagina, tamano, orden)),
                WorkerMapper::toDomain);
    }

    @Override
    public ResumenJornales resumenPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        List<Object[]> filas = jpa.resumenPorTrabajador(trabajadorId, inicio, fin);
        Object[] f = filas.isEmpty() ? new Object[6] : filas.get(0);
        return new ResumenJornales(
                f[0] == null ? 0 : ((Number) f[0]).longValue(), decimal(f[1]),
                f[2] == null ? 0 : ((Number) f[2]).longValue(), decimal(f[3]),
                (LocalDate) f[4], (LocalDate) f[5]);
    }

    private static <E, D> Pagina<D> aPagina(Page<E> p, java.util.function.Function<E, D> f) {
        return new Pagina<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }

    private static BigDecimal decimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }
}
