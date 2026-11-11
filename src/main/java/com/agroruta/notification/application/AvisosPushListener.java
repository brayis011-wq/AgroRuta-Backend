package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.AvisosPushUseCase;
import com.agroruta.notification.application.ports.in.PreferenciasCorreoUseCase;
import com.agroruta.notification.domain.AvisoPush;
import com.agroruta.shared.evento.AccesoPortalSolicitado;
import com.agroruta.shared.evento.PagoRegistrado;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Usuario;
import com.agroruta.worker.application.ports.out.PagoRepository;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Trabajador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lo que pasa en la app, como aviso en el teléfono de quien activó los avisos push.
 * Igual que los correos: en segundo plano, solo si la operación quedó guardada y sin afectarla si falla.
 */
@Component
public class AvisosPushListener {

    private static final Logger log = LoggerFactory.getLogger(AvisosPushListener.class);

    private final UsuarioRepository usuarios;
    private final TrabajadorRepository trabajadores;
    private final PagoRepository pagos;
    private final PreferenciasCorreoUseCase preferencias;
    private final AvisosPushUseCase push;

    public AvisosPushListener(UsuarioRepository usuarios, TrabajadorRepository trabajadores, PagoRepository pagos,
                              PreferenciasCorreoUseCase preferencias, AvisosPushUseCase push) {
        this.usuarios = usuarios;
        this.trabajadores = trabajadores;
        this.pagos = pagos;
        this.preferencias = preferencias;
        this.push = push;
    }

    /** Al trabajador: "te pagaron" */
    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void pagoRegistrado(PagoRegistrado e) {
        seguro(() -> pagos.buscarPorId(e.pagoId()).ifPresent(p -> {
            Nomina n = p.getNomina();
            Trabajador t = n == null || n.getTrabajador() == null ? null
                    : trabajadores.buscarPorId(n.getTrabajador().getId()).orElse(n.getTrabajador());
            if (t == null || !t.tieneAccesoAprobado() || t.getUsuarioId() == null) return;
            usuarios.findById(t.getUsuarioId()).filter(Usuario::isActivo).ifPresent(u ->
                    push.enviar(u.getId(), new AvisoPush("Te registraron un pago",
                            "Recibiste " + MensajesCorreo.pesos(p.getMonto()) + ". Mira el comprobante en tu portal.",
                            "/portal/payments", "pago-" + p.getId())));
        }));
    }

    /** Al agricultor: un trabajador pidió entrar a su portal */
    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void accesoSolicitado(AccesoPortalSolicitado e) {
        seguro(() -> trabajadores.buscarPorId(e.trabajadorId()).ifPresent(t -> {
            if (t.getAgricultorId() == null) return;
            usuarios.findById(t.getAgricultorId()).filter(Usuario::isActivo)
                    .filter(a -> preferencias.obtener(a.getId()).avisosTrabajadores())
                    .ifPresent(a -> push.enviar(a.getId(), new AvisoPush("Solicitud de acceso al portal",
                            t.getNombreCompleto() + " quiere ver sus jornales y pagos. Apruébalo o recházalo.",
                            "/worker", "acceso-" + t.getId())));
        }));
    }

    private void seguro(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException ex) {
            log.warn("No se pudo preparar un aviso push", ex);
        }
    }
}
