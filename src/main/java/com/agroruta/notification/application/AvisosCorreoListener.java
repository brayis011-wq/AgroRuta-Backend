package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.PreferenciasCorreoUseCase;
import com.agroruta.notification.application.ports.out.CorreoPort;
import com.agroruta.shared.evento.AccesoPortalResuelto;
import com.agroruta.shared.evento.AccesoPortalSolicitado;
import com.agroruta.shared.evento.ContrasenaCambiada;
import com.agroruta.shared.evento.CuentaBloqueada;
import com.agroruta.shared.evento.PagoRegistrado;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.PagoRepository;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.domain.TrabajadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Convierte lo que pasa en la app en correos. Corre en segundo plano y solo cuando la operación
 * ya quedó guardada (si la transacción falla, no sale ningún correo). Un correo que falla nunca
 * afecta la operación del usuario.
 */
@Component
public class AvisosCorreoListener {

    private static final Logger log = LoggerFactory.getLogger(AvisosCorreoListener.class);

    private final UsuarioRepository usuarios;
    private final TrabajadorRepository trabajadores;
    private final PagoRepository pagos;
    private final PreferenciasCorreoUseCase preferencias;
    private final MensajesCorreo mensajes;
    private final CorreoPort correo;
    private final Clock reloj = Clock.system(ZoneId.of("America/Bogota"));

    public AvisosCorreoListener(UsuarioRepository usuarios, TrabajadorRepository trabajadores, PagoRepository pagos,
                                PreferenciasCorreoUseCase preferencias, MensajesCorreo mensajes, CorreoPort correo) {
        this.usuarios = usuarios;
        this.trabajadores = trabajadores;
        this.pagos = pagos;
        this.preferencias = preferencias;
        this.mensajes = mensajes;
        this.correo = correo;
    }

    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void cuentaBloqueada(CuentaBloqueada e) {
        seguro(() -> usuarios.findByEmail(e.email()).filter(Usuario::isActivo).ifPresent(u ->
                correo.enviar(mensajes.cuentaBloqueada(u.getNombre(), u.getEmail(), e.minutos()))));
    }

    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void contrasenaCambiada(ContrasenaCambiada e) {
        seguro(() -> usuarios.findById(e.usuarioId()).ifPresent(u ->
                correo.enviar(mensajes.contrasenaCambiada(u.getNombre(), u.getEmail(), LocalDateTime.now(reloj)))));
    }

    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void accesoSolicitado(AccesoPortalSolicitado e) {
        seguro(() -> trabajadores.buscarPorId(e.trabajadorId()).ifPresent(t -> {
            if (t.getAgricultorId() == null) return;
            usuarios.findById(t.getAgricultorId()).filter(Usuario::isActivo)
                    .filter(a -> preferencias.obtener(a.getId()).avisosTrabajadores())
                    .ifPresent(a -> correo.enviar(mensajes.solicitudAcceso(a.getNombre(), a.getEmail(),
                            t.getNombreCompleto(), t.getAccesoNombre(), t.getAccesoEmail())));
        }));
    }

    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void accesoResuelto(AccesoPortalResuelto e) {
        seguro(() -> trabajadores.buscarPorId(e.trabajadorId()).ifPresent(t ->
                cuentaDe(t).ifPresent(u -> {
                    String empleador = t.getAgricultorId() == null ? null
                            : usuarios.findById(t.getAgricultorId()).map(Usuario::getNombre).orElse(null);
                    correo.enviar(mensajes.accesoResuelto(u.getNombre(), u.getEmail(), e.aprobado(), e.retirado(), empleador));
                })));
    }

    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void pagoRegistrado(PagoRegistrado e) {
        seguro(() -> pagos.buscarPorId(e.pagoId()).ifPresent(p -> {
            Nomina n = p.getNomina();
            Trabajador t = n == null || n.getTrabajador() == null ? null
                    : trabajadores.buscarPorId(n.getTrabajador().getId()).orElse(n.getTrabajador());
            if (t == null || !t.tieneAccesoAprobado()) return; // sin portal no tiene a quién avisar
            cuentaDe(t).ifPresent(u -> correo.enviar(mensajes.pagoRegistrado(u.getNombre(), u.getEmail(),
                    n.getPeriodoInicio(), n.getPeriodoFin(), p.getMonto(),
                    p.getMetodoPago() == null ? null : p.getMetodoPago().name(), p.getFechaPago())));
        }));
    }

    /** La cuenta de usuario que tiene (o pidió) acceso a este trabajador */
    private Optional<Usuario> cuentaDe(Trabajador t) {
        if (t.getUsuarioId() == null) return Optional.empty();
        return usuarios.findById(t.getUsuarioId()).filter(Usuario::isActivo);
    }

    private void seguro(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException ex) {
            log.warn("No se pudo preparar un correo de aviso", ex);
        }
    }
}
