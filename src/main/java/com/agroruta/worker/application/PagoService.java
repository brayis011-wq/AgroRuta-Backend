package com.agroruta.worker.application;

import com.agroruta.worker.application.ports.in.PagoUseCase;
import com.agroruta.worker.domain.NominaRepository;
import com.agroruta.worker.domain.PagoRepository;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.MetodoPago;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import com.agroruta.worker.application.ports.in.PagoUseCase;
@Service
public class PagoService implements PagoUseCase {

    private final PagoRepository pagoRepository;
    private final NominaRepository nominaRepository;

    public PagoService(PagoRepository pagoRepository, NominaRepository nominaRepository) {
        this.pagoRepository = pagoRepository;
        this.nominaRepository = nominaRepository;
    }

    public Pago registrarPago(Long nominaId, BigDecimal monto,
                              MetodoPago metodoPago, String comprobante,
                              String observaciones) {

        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new IllegalArgumentException("Nómina no encontrada con id: " + nominaId));

        if (!EstadoNomina.PAGADA.equals(nomina.getEstado())) {
            throw new IllegalStateException("Solo se puede registrar el pago de nóminas aprobadas.");
        }

        pagoRepository.buscarPorNomina(nominaId).ifPresent(p -> {
            throw new IllegalStateException("Esta nómina ya tiene un pago registrado.");
        });

        Pago pago = new Pago(null, nomina, LocalDate.now(), monto, metodoPago, comprobante);
        pago.setObservaciones(observaciones);

        if (!pago.montoEsCorrecto()) {
            throw new IllegalArgumentException(
                    "El monto del pago (" + monto + ") no coincide con el total de la nómina ("
                            + nomina.getValorTotal() + ")."
            );
        }

        return pagoRepository.guardar(pago);
    }

    public Pago buscarPorId(Long id) {
        return pagoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con id: " + id));
    }

    public List<Pago> historialPorTrabajador(Long trabajadorId) {
        return pagoRepository.buscarPorTrabajador(trabajadorId);
    }

    public List<Pago> listarTodos() {
        return pagoRepository.listarTodos();
    }
}