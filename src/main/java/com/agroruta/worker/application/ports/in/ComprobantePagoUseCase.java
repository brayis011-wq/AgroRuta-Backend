package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.ComprobantePago;

import java.util.Optional;

/** Comprobante de pago en PDF, con un QR para comprobar que es auténtico. */
public interface ComprobantePagoUseCase {

    /** PDF del comprobante (quien llama ya comprobó que el pago es suyo). */
    byte[] pdf(Long pagoId);

    /** PDF para el portal del trabajador: solo si el pago es de ese trabajador y su acceso está aprobado. */
    byte[] pdfDelTrabajador(Long usuarioId, Long pagoId);

    /** Los datos del comprobante si el código del QR es el de ese pago; vacío si no coincide. */
    Optional<ComprobantePago> verificar(Long pagoId, String codigo);
}
