package com.agroruta.worker.application;

import com.agroruta.worker.domain.PagosDelMes;
import com.agroruta.worker.domain.ResumenPagos;
import com.agroruta.shared.util.Pagina;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.PagoUseCase;
import com.agroruta.worker.application.ports.out.JornalRepository;
import com.agroruta.worker.application.ports.out.NominaRepository;
import com.agroruta.worker.application.ports.out.PagoRepository;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.MetodoPago;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.agroruta.shared.evento.PagoRegistrado;
import org.springframework.context.ApplicationEventPublisher;

@Service
@Transactional
public class PagoService implements PagoUseCase {

    private final PagoRepository pagoRepository;
    private final NominaRepository nominaRepository;
    private final JornalRepository jornalRepository;
    private final ApplicationEventPublisher eventos;

    public PagoService(PagoRepository pagoRepository,
                       NominaRepository nominaRepository,
                       JornalRepository jornalRepository,
                       ApplicationEventPublisher eventos) {
        this.pagoRepository = pagoRepository;
        this.nominaRepository = nominaRepository;
        this.jornalRepository = jornalRepository;
        this.eventos = eventos;
    }

    /**
     * Registra el pago de una nómina APROBADA y la deja PAGADA, con sus jornales liquidados.
     * Todo ocurre en una sola transacción: si algo falla, la nómina sigue APROBADA.
     *
     * Compatibilidad: las nóminas que el flujo anterior dejó PAGADA sin pago registrado
     * también aceptan el pago, sin volver a cambiar su estado.
     */
    @Override
    public Pago registrarPago(Long nominaId, BigDecimal monto,
                              MetodoPago metodoPago, String comprobante,
                              String observaciones) {

        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nómina no encontrada con id: " + nominaId));

        boolean aprobada = EstadoNomina.APROBADA.equals(nomina.getEstado());
        boolean pagadaSinPago = EstadoNomina.PAGADA.equals(nomina.getEstado());
        if (!aprobada && !pagadaSinPago) {
            throw new IllegalStateException(
                    "Solo se puede registrar el pago de nóminas en estado APROBADA.");
        }

        // Evitar doble pago
        pagoRepository.buscarPorNomina(nominaId).ifPresent(p -> {
            throw new IllegalStateException("Esta nómina ya tiene un pago registrado.");
        });

        if (monto == null || nomina.getValorTotal() == null
                || monto.compareTo(nomina.getValorTotal()) != 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser igual al valor total de la nómina ("
                            + nomina.getValorTotal() + ").");
        }

        if (aprobada) {
            nomina.marcarComoPagada();  // APROBADA → PAGADA y jornales liquidados en memoria
            nomina.getJornales().forEach(jornalRepository::guardar);
            nominaRepository.guardar(nomina);
        }

        Pago pago = new Pago(null, nomina, LocalDate.now(), monto, metodoPago, comprobante);
        pago.setObservaciones(observaciones);

        Pago guardado = pagoRepository.guardar(pago);
        if (guardado != null && guardado.getId() != null) {
            eventos.publishEvent(new PagoRegistrado(guardado.getId()));
        }
        return guardado;
    }

    @Override
    public Pago buscarPorId(Long id) {
        return pagoRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pago no encontrado con id: " + id));
    }

    @Override
    public List<Pago> historialPorTrabajador(Long trabajadorId) {
        return pagoRepository.buscarPorTrabajador(trabajadorId);
    }

    @Override
    public List<Pago> listarTodos() {
        return pagoRepository.listarTodos();
    }
    @Override
    public List<Pago> listarTodosDe(Long agricultorId) {
        if (agricultorId == null) return List.of();
        return pagoRepository.listarPorAgricultor(agricultorId);
    }

    @Override
    public Pagina<Pago> pagina(Long agricultorId, Long nominaId, int pagina, int tamano) {
        return pagoRepository.pagina(agricultorId, nominaId, pagina, tamano);
    }

    @Override
    public ResumenPagos resumen(Long agricultorId, Long nominaId) {
        return pagoRepository.resumen(agricultorId, nominaId);
    }

    @Override
    public List<PagosDelMes> totalesPorMes(Long agricultorId, LocalDate desde) {
        return pagoRepository.totalesPorMes(agricultorId, desde);
    }
}
