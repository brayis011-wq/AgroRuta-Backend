package com.agroruta.worker.application.ports.out;

import com.agroruta.worker.domain.Pago;

import java.util.List;
import java.util.Optional;

public interface PagoRepository {

    Pago guardar(Pago pago);
    Optional<Pago> buscarPorId(Long id);
    Optional<Pago> buscarPorNomina(Long nominaId);
    List<Pago> buscarPorTrabajador(Long trabajadorId);
    List<Pago> listarTodos();
    List<Pago> listarPorAgricultor(Long agricultorId);
}