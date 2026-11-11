package com.agroruta.worker.application.ports.out;

import com.agroruta.worker.domain.ComprobantePago;

/** Dibuja el comprobante en PDF. Lo implementa el módulo de reportes, que ya tiene el lienzo. */
public interface ComprobantePdfPort {
    byte[] generar(ComprobantePago comprobante);
}
