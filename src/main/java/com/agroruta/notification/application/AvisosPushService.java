package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.AvisosPushUseCase;
import com.agroruta.notification.application.ports.out.ClavesPushRepository;
import com.agroruta.notification.application.ports.out.PushPort;
import com.agroruta.notification.application.ports.out.SuscripcionPushRepository;
import com.agroruta.notification.domain.AvisoPush;
import com.agroruta.notification.domain.CifradoWebPush;
import com.agroruta.notification.domain.ClavesVapid;
import com.agroruta.notification.domain.SuscripcionPush;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.util.CifradoLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Avisos push con el estándar Web Push: sin servicios de pago ni cuentas externas.
 * El servidor genera su par de claves la primera vez y lo guarda cifrado en la base.
 */
@Service
public class AvisosPushService implements AvisosPushUseCase {

    private static final Logger log = LoggerFactory.getLogger(AvisosPushService.class);

    /**
     * Servicios de avisos de los navegadores. El endpoint lo manda el navegador, así que solo se
     * aceptan estos: de lo contrario alguien podría hacer que el servidor llame a cualquier dirección.
     */
    private static final List<String> SERVICIOS = List.of(
            "fcm.googleapis.com", "push.services.mozilla.com", "notify.windows.com", "push.apple.com");
    private static final int MAX_ENDPOINT = 700;
    private static final int MAX_DISPOSITIVOS = 10;
    private static final long VALIDEZ_FIRMA_SEGUNDOS = 12 * 3600;

    private final SuscripcionPushRepository suscripciones;
    private final ClavesPushRepository clavesGuardadas;
    private final PushPort push;
    private final CifradoLocal cifrado;
    private final String contacto;
    private ClavesVapid claves;

    public AvisosPushService(SuscripcionPushRepository suscripciones, ClavesPushRepository clavesGuardadas, PushPort push,
                             @Value("${jwt.secret}") String secreto,
                             @Value("${app.push.contacto:mailto:soporte@agroruta.app}") String contacto) {
        this.suscripciones = suscripciones;
        this.clavesGuardadas = clavesGuardadas;
        this.push = push;
        this.cifrado = new CifradoLocal(secreto, "agroruta/avisos-push/v1");
        this.contacto = contacto;
    }

    @Override
    public String clavePublica() {
        return claves().publica();
    }

    @Override
    public void suscribir(Long usuarioId, String endpoint, String p256dh, String auth, String dispositivo) {
        if (!endpointPermitido(endpoint)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Este navegador usa un servicio de avisos que no reconocemos.");
        }
        if (!CifradoWebPush.clavesValidas(p256dh, auth)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Las claves de la suscripción no son válidas.");
        }
        suscripciones.guardar(usuarioId, endpoint, p256dh, auth, dispositivo);
        // Un teléfono viejo que ya no se usa no debe quedarse recibiendo avisos para siempre
        List<SuscripcionPush> todas = suscripciones.deUsuario(usuarioId);
        todas.stream().skip(MAX_DISPOSITIVOS).forEach(s -> suscripciones.eliminar(s.id()));
    }

    @Override
    public void desuscribir(Long usuarioId, String endpoint) {
        suscripciones.eliminarPorEndpoint(usuarioId, endpoint);
    }

    @Override
    public boolean tieneSuscripcion(Long usuarioId) {
        return suscripciones.existe(usuarioId);
    }

    @Override
    public int enviar(Long usuarioId, AvisoPush aviso) {
        int entregados = 0;
        try {
            List<SuscripcionPush> destinos = suscripciones.deUsuario(usuarioId);
            if (destinos.isEmpty()) return 0;
            ClavesVapid c = claves();
            byte[] json = aviso.json();
            long expira = Instant.now().getEpochSecond() + VALIDEZ_FIRMA_SEGUNDOS;
            for (SuscripcionPush s : destinos) {
                try {
                    if (!endpointPermitido(s.endpoint())) {
                        suscripciones.eliminar(s.id());
                        continue;
                    }
                    String servicio = "https://" + URI.create(s.endpoint()).getHost();
                    PushPort.Resultado r = push.enviar(s.endpoint(), c.autorizacion(servicio, contacto, expira),
                            CifradoWebPush.cifrar(json, s.p256dh(), s.auth()));
                    if (r == PushPort.Resultado.ENTREGADO) entregados++;
                    else if (r == PushPort.Resultado.CADUCADA) suscripciones.eliminar(s.id());
                } catch (RuntimeException e) {
                    log.warn("No se pudo mandar el aviso push a la suscripción {}: {}", s.id(), e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            log.warn("No se pudieron mandar los avisos push del usuario {}", usuarioId, e);
        }
        return entregados;
    }

    static boolean endpointPermitido(String endpoint) {
        if (endpoint == null || endpoint.length() > MAX_ENDPOINT) return false;
        try {
            URI u = URI.create(endpoint);
            String host = u.getHost();
            if (!"https".equalsIgnoreCase(u.getScheme()) || host == null || u.getUserInfo() != null) return false;
            if (u.getPort() != -1 && u.getPort() != 443) return false;
            String h = host.toLowerCase(java.util.Locale.ROOT);
            return SERVICIOS.stream().anyMatch(s -> h.equals(s) || h.endsWith("." + s));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Las claves del servidor: se leen una vez; si no existen (o ya no se pueden leer) se generan. */
    private synchronized ClavesVapid claves() {
        if (claves != null) return claves;
        var guardadas = clavesGuardadas.buscar();
        if (guardadas.isPresent()) {
            try {
                claves = new ClavesVapid(guardadas.get().publica(), cifrado.descifrar(guardadas.get().privadaCifrada()));
                return claves;
            } catch (IllegalStateException e) {
                // Cambió JWT_SECRET: la clave privada ya no se puede leer. Las suscripciones hechas
                // con la clave anterior no sirven con una nueva, así que se borran y cada quien vuelve a activar.
                log.warn("Las claves de los avisos push no se pueden leer (¿cambió JWT_SECRET?). Se generan nuevas "
                        + "y se borran las suscripciones anteriores.");
                suscripciones.eliminarTodas();
            }
        }
        ClavesVapid nuevas = ClavesVapid.generar();
        clavesGuardadas.guardar(nuevas.publica(), cifrado.cifrar(nuevas.privada()));
        claves = nuevas;
        return claves;
    }
}
