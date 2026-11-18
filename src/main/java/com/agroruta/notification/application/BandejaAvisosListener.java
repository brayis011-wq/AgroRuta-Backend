package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.BandejaAvisosUseCase;
import com.agroruta.notification.domain.AvisoBandeja;
import com.agroruta.shared.evento.AccesoPortalResuelto;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lo que pasa en la app, guardado en la bandeja (la campana) de quien le toca.
 * A diferencia del correo y del push, se guarda siempre: no depende de preferencias ni de dispositivos.
 *
 * En segundo plano (@Async) como los demás avisos: así escribe en su propia transacción, después de
 * que la operación quedó guardada, y si falla no la afecta.
 */
@Component
public class BandejaAvisosListener {

    private static final Logger log = LoggerFactory.getLogger(BandejaAvisosListener.class);

    private final UsuarioRepository usuarios;
    private final TrabajadorRepository trabajadores;
    private final PagoRepository pagos;
    private final BandejaAvisosUseCase bandeja;

    public BandejaAvisosListener(UsuarioRepository usuarios, TrabajadorRepository trabajadores, PagoRepository pagos,
                                 BandejaAvisosUseCase bandeja) {
        this.usuarios = usuarios;
        this.trabajadores = trabajadores;
        this.pagos = pagos;
        this.bandeja = bandeja;
    }

    /** Al trabajador con portal: "te registraron un pago" */
    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void pagoRegistrado(PagoRegistrado e) {
        seguro(() -> pagos.buscarPorId(e.pagoId()).ifPresent(p -> {
            Nomina n = p.getNomina();
            Trabajador t = n == null || n.getTrabajador() == null ? null
                    : trabajadores.buscarPorId(n.getTrabajador().getId()).orElse(n.getTrabajador());
            if (t == null || !t.tieneAccesoAprobado() || t.getUsuarioId() == null) return;
            usuarios.findById(t.getUsuarioId()).filter(Usuario::isActivo).ifPresent(u -> {
                Map<String, Object> datos = new LinkedHashMap<>();
                datos.put("monto", p.getMonto() == null ? null : p.getMonto().toPlainString());
                if (n.getPeriodoInicio() != null) datos.put("inicio", n.getPeriodoInicio().toString());
                if (n.getPeriodoFin() != null) datos.put("fin", n.getPeriodoFin().toString());
                bandeja.avisar(new AvisoBandeja.Nuevo(u.getId(), AvisoBandeja.Tipo.PAGO_REGISTRADO,
                        "Te registraron un pago",
                        "Recibiste " + MensajesCorreo.pesos(p.getMonto()) + ". Mira el comprobante en tus pagos.",
                        datos, "/portal/payments", "pago-" + p.getId()));
            });
        }));
    }

    /** Al agricultor: un trabajador pidió entrar a su portal */
    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void accesoSolicitado(AccesoPortalSolicitado e) {
        seguro(() -> trabajadores.buscarPorId(e.trabajadorId()).ifPresent(t -> {
            if (t.getAgricultorId() == null) return;
            usuarios.findById(t.getAgricultorId()).filter(Usuario::isActivo).ifPresent(a ->
                    bandeja.avisar(new AvisoBandeja.Nuevo(a.getId(), AvisoBandeja.Tipo.ACCESO_SOLICITADO,
                            "Solicitud de acceso al portal",
                            t.getNombreCompleto() + " quiere ver sus jornales y pagos. Apruébalo o recházalo.",
                            Map.of("trabajador", texto(t.getNombreCompleto())),
                            "/worker", "acceso-" + t.getId())));
        }));
    }

    /** Al trabajador: le aprobaron, rechazaron o retiraron el acceso */
    @Async("correos")
    @TransactionalEventListener(fallbackExecution = true)
    public void accesoResuelto(AccesoPortalResuelto e) {
        seguro(() -> trabajadores.buscarPorId(e.trabajadorId()).ifPresent(t -> {
            if (t.getUsuarioId() == null) return;
            usuarios.findById(t.getUsuarioId()).filter(Usuario::isActivo).ifPresent(u -> {
                String empleador = t.getAgricultorId() == null ? ""
                        : usuarios.findById(t.getAgricultorId()).map(Usuario::getNombre).orElse("");
                String titulo = e.aprobado() ? "Ya puedes entrar a tu portal"
                        : e.retirado() ? "Tu acceso al portal fue retirado" : "Tu solicitud no fue aprobada";
                String cuerpo = e.aprobado() ? "Ya puedes ver tus jornales, nóminas y pagos."
                        : "Si crees que es un error, habla con tu empleador.";
                bandeja.avisar(new AvisoBandeja.Nuevo(u.getId(), AvisoBandeja.Tipo.ACCESO_RESUELTO, titulo, cuerpo,
                        Map.of("aprobado", e.aprobado(), "retirado", e.retirado(), "empleador", empleador),
                        "/portal/home", "acceso-resuelto-" + t.getId()));
            });
        }));
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }

    private void seguro(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException ex) {
            log.warn("No se pudo preparar un aviso de la bandeja", ex);
        }
    }
}
