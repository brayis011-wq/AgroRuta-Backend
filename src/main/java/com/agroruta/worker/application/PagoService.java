package com.agroruta.worker.application;

import com.agroRuta.worker.application.ports.NominaRepository;
import com.agroRuta.worker.application.ports.PagoRepository;
import com.agroRuta.worker.domain.EstadoNomina;
import com.agroRuta.worker.domain.MetodoPago;
import com.agroRuta.worker.domain.Nomina;
import com.agroRuta.worker.domain.Pago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PagoService {

    private final PagoRepository pagoRepository;
    private final NominaRepository nominaRepository;

    public PagoService(PagoRepository pagoRepository, NominaRepository nominaRepository) {
        this.pagoRepository = pagoRepository;
        this.nominaRepository = nominaRepository;
    }

    /**
     * Registra el pago efectivo de una nómina.
     * La nómina debe estar en estado PAGADA (aprobada previamente en NominaService).
     */
    public Pago registrarPago(Long nominaId, BigDecimal monto,
                              MetodoPago metodoPago, String comprobante,
                              String observaciones) {
        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new IllegalArgumentException("Nómina no encontrada con id: " + nominaId));

        if (!EstadoNomina.PAGADA.equals(nomina.getEstado())) {
            throw new IllegalStateException("Solo se puede registrar el pago de nóminas aprobadas.");
        }

        // Verificar que ya no tenga un pago registrado
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
