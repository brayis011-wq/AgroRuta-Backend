package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.MetodoPago;
import com.agroruta.worker.domain.Pago;

import java.math.BigDecimal;
import java.util.List;

public interface PagoUseCase {

    Pago registrarPago(Long nominaId, BigDecimal monto,
                       MetodoPago metodoPago, String comprobante,
                       String observaciones);

    Pago buscarPorId(Long id);

    List<Pago> historialPorTrabajador(Long trabajadorId);

    List<Pago> listarTodos();
}