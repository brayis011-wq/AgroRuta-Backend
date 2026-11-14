package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.PagosDelMes;
import java.util.Comparator;
import java.time.YearMonth;
import java.time.LocalDate;
import com.agroruta.worker.domain.ResumenPagos;
import java.math.BigDecimal;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import com.agroruta.shared.util.Pagina;

import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.application.ports.out.PagoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    // Con transacción propia: al armar el pago se leen los jornales de su nómina (carga diferida),
    // y los avisos de "pago registrado" llaman esto desde otro hilo, sin la sesión de la petición.
    @Transactional(readOnly = true)
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
    @Override public List<Pago> listarPorAgricultor(Long agricultorId) {
        return jpa.findByAgricultorId(agricultorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Pagina<Pago> pagina(Long agricultorId, Long nominaId, int pagina, int tamano) {
        var orden = Sort.by(Sort.Order.desc("fechaPago"), Sort.Order.desc("id"));
        return aPagina(jpa.pagina(agricultorId, nominaId, PageRequest.of(pagina, tamano, orden)), WorkerMapper::toDomain);
    }

    private static <E, D> Pagina<D> aPagina(Page<E> p, java.util.function.Function<E, D> f) {
        return new Pagina<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }

    private static BigDecimal decimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }

    @Override
    public ResumenPagos resumen(Long agricultorId, Long nominaId) {
        List<Object[]> filas = jpa.resumen(agricultorId, nominaId);
        Object[] f = filas.isEmpty() ? new Object[3] : filas.get(0);
        return new ResumenPagos(f[0] == null ? 0 : ((Number) f[0]).longValue(), decimal(f[1]),
                f[2] == null ? 0 : ((Number) f[2]).longValue());
    }

    @Override
    public List<PagosDelMes> totalesPorMes(Long agricultorId, LocalDate desde) {
        // Cada fila: [año, mes, total, pagos]
        return jpa.totalesPorMes(agricultorId, desde).stream()
                .map(f -> new PagosDelMes(
                        YearMonth.of(((Number) f[0]).intValue(), ((Number) f[1]).intValue()).toString(),
                        decimal(f[2]), ((Number) f[3]).longValue()))
                .sorted(Comparator.comparing(PagosDelMes::mes))
                .toList();
    }
}
