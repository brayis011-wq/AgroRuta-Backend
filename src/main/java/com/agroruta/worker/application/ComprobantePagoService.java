package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.ComprobantePagoUseCase;
import com.agroruta.worker.application.ports.out.ComprobantePdfPort;
import com.agroruta.worker.application.ports.out.EmpleadorPort;
import com.agroruta.worker.application.ports.out.PagoRepository;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.worker.domain.ComprobantePago;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.Trabajador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ComprobantePagoService implements ComprobantePagoUseCase {

    private final PagoRepository pagoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final EmpleadorPort empleadorPort;
    private final ComprobantePdfPort pdfPort;
    private final byte[] clave;
    private final String frontend;

    public ComprobantePagoService(PagoRepository pagoRepository,
                                  TrabajadorRepository trabajadorRepository,
                                  EmpleadorPort empleadorPort,
                                  ComprobantePdfPort pdfPort,
                                  @Value("${jwt.secret}") String secreto,
                                  @Value("${app.frontend-url:http://localhost:4200}") String frontend) {
        this.pagoRepository = pagoRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.empleadorPort = empleadorPort;
        this.pdfPort = pdfPort;
        // Clave propia para los comprobantes, derivada del secreto del servidor: así un código
        // de comprobante nunca sirve como firma de otra cosa (ni al revés)
        this.clave = hmac(secreto.getBytes(StandardCharsets.UTF_8), "agroruta/comprobante-pago/v1");
        this.frontend = frontend.endsWith("/") ? frontend.substring(0, frontend.length() - 1) : frontend;
    }

    @Override
    public byte[] pdf(Long pagoId) {
        return pdfPort.generar(comprobante(buscar(pagoId)));
    }

    @Override
    public byte[] pdfDelTrabajador(Long usuarioId, Long pagoId) {
        Trabajador yo = trabajadorRepository.buscarPorUsuarioId(usuarioId)
                .filter(Trabajador::tieneAccesoAprobado)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "Tu acceso todavía no ha sido aprobado por tu empleador."));
        Pago pago = buscar(pagoId);
        Trabajador delPago = pago.getNomina() == null ? null : pago.getNomina().getTrabajador();
        // El mismo mensaje que si no existiera: no se confirma que el pago de otra persona existe
        if (delPago == null || !yo.getId().equals(delPago.getId())) throw noEncontrado(pagoId);
        return pdfPort.generar(comprobante(pago));
    }

    @Override
    public Optional<ComprobantePago> verificar(Long pagoId, String codigo) {
        if (pagoId == null || codigo == null || codigo.isBlank()) return Optional.empty();
        return pagoRepository.buscarPorId(pagoId)
                .filter(p -> MessageDigest.isEqual(
                        codigo(p).getBytes(StandardCharsets.UTF_8), codigo.trim().getBytes(StandardCharsets.UTF_8)))
                .map(this::comprobante);
    }

    private Pago buscar(Long pagoId) {
        return pagoRepository.buscarPorId(pagoId).orElseThrow(() -> noEncontrado(pagoId));
    }

    private static ResourceNotFoundException noEncontrado(Long pagoId) {
        return new ResourceNotFoundException("Pago no encontrado con ID: " + pagoId);
    }

    private ComprobantePago comprobante(Pago p) {
        Nomina n = p.getNomina();
        Trabajador t = n == null ? null : n.getTrabajador();
        String codigo = codigo(p);
        return new ComprobantePago(
                p.getId(), ComprobantePago.numeroDe(p.getId()), p.getFechaPago(), p.getMonto(), p.getMetodoPago(),
                p.getComprobante(), p.getObservaciones(),
                t == null ? null : nombreCompleto(t),
                t == null ? null : t.getCedula(),
                t == null ? null : empleadorPort.nombre(t.getAgricultorId()).orElse(null),
                n == null ? null : n.getPeriodoInicio(),
                n == null ? null : n.getPeriodoFin(),
                n == null ? 0 : n.getTotalJornales(),
                codigo,
                frontend + "/receipt/" + p.getId() + "/" + codigo);
    }

    private static String nombreCompleto(Trabajador t) {
        String nombre = t.getNombre() == null ? "" : t.getNombre().trim();
        String apellido = t.getApellido() == null ? "" : t.getApellido().trim();
        return (nombre + " " + apellido).trim();
    }

    /**
     * Firma del pago: 16 letras que salen del id, el valor y la fecha. Si alguien cambia el valor
     * en el PDF, el código del QR deja de coincidir con lo que tiene el servidor.
     */
    String codigo(Pago p) {
        String datos = p.getId() + "|"
                + (p.getMonto() == null ? "" : p.getMonto().stripTrailingZeros().toPlainString()) + "|"
                + (p.getFechaPago() == null ? "" : p.getFechaPago());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(hmac(clave, datos), 12));
    }

    private static byte[] hmac(byte[] clave, String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el comprobante", e);
        }
    }
}
