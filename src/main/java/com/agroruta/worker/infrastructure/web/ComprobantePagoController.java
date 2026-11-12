package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.application.ports.in.ComprobantePagoUseCase;
import com.agroruta.worker.domain.ComprobantePago;
import com.agroruta.worker.infrastructure.web.dto.ComprobanteVerificadoResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Comprobante de pago: PDF para el agricultor y para el trabajador, y verificación pública del QR. */
@RestController
public class ComprobantePagoController {

    private final ComprobantePagoUseCase comprobantes;

    public ComprobantePagoController(ComprobantePagoUseCase comprobantes) {
        this.comprobantes = comprobantes;
    }

    @GetMapping(value = "/api/payments/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@ownership.pagoPropio(#id, authentication)")
    public ResponseEntity<byte[]> delAgricultor(@PathVariable Long id) {
        return pdf(id, comprobantes.pdf(id));
    }

    /** El trabajador descarga el comprobante de uno de sus propios pagos. */
    @GetMapping(value = "/api/portal/payments/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('Trabajador')")
    public ResponseEntity<byte[]> delTrabajador(@PathVariable Long id, Authentication auth) {
        return pdf(id, comprobantes.pdfDelTrabajador(CurrentUser.id(auth), id));
    }

    /**
     * Pública (sin sesión): la abre quien escanea el QR. Solo responde si el código es el del pago;
     * un código equivocado y un pago que no existe dan la misma respuesta.
     */
    @GetMapping("/api/public/receipts/{id}")
    public ResponseEntity<ComprobanteVerificadoResponse> verificar(@PathVariable Long id, @RequestParam String code) {
        ComprobantePago c = comprobantes.verificar(id, code)
                .orElseThrow(() -> new ResourceNotFoundException("Este comprobante no es válido."));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ComprobanteVerificadoResponse.from(c));
    }

    private static ResponseEntity<byte[]> pdf(Long id, byte[] contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"comprobante-" + ComprobantePago.numeroDe(id) + ".pdf\"")
                .cacheControl(CacheControl.noStore())
                .body(contenido);
    }
}
