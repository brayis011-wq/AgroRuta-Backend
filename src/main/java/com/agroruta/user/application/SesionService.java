package com.agroruta.user.application;

import com.agroruta.shared.evento.ContrasenaCambiada;
import com.agroruta.shared.exception.SesionInvalidaException;
import com.agroruta.user.application.ports.in.SesionUseCase;
import com.agroruta.user.application.ports.out.SesionRepository;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Sesion;
import com.agroruta.user.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SesionService implements SesionUseCase {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    /** Margen para que dos pestañas renueven con el mismo token sin que parezca un robo. */
    static final Duration MARGEN_ROTACION = Duration.ofSeconds(60);

    private static final String TERMINO = "Tu sesión terminó. Vuelve a iniciar sesión.";

    private final SesionRepository sesiones;
    private final UsuarioRepository usuarios;
    private final Duration duracion;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    @Autowired
    public SesionService(SesionRepository sesiones, UsuarioRepository usuarios,
                         @Value("${app.sesion.duracion-dias:30}") long duracionDias) {
        this(sesiones, usuarios, Duration.ofDays(duracionDias), Clock.systemDefaultZone());
    }

    SesionService(SesionRepository sesiones, UsuarioRepository usuarios, Duration duracion, Clock reloj) {
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.duracion = duracion;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public String abrir(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        sesiones.borrarVencidas(usuarioId, ahora);
        String token = nuevoToken();
        sesiones.guardar(Sesion.nueva(usuarioId, hash(token), ahora, duracion));
        return token;
    }

    // noRollbackFor: si se detecta un token robado, el cierre de todas las sesiones debe quedar guardado
    @Override
    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public Renovacion renovar(String token) {
        if (token == null || token.isBlank()) throw new SesionInvalidaException(TERMINO);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        Sesion sesion = sesiones.buscarPorTokenHash(hash(token))
                .orElseThrow(() -> new SesionInvalidaException(TERMINO));
        if (sesion.revocada() || sesion.vencida(ahora)) throw new SesionInvalidaException(TERMINO);

        if (sesion.rotada() && !sesion.rotadaHaceMenosDe(MARGEN_ROTACION, ahora)) {
            log.warn("Token de sesión reutilizado para el usuario {}: se cierran todas sus sesiones", sesion.getUsuarioId());
            sesiones.revocarTodas(sesion.getUsuarioId(), ahora);
            throw new SesionInvalidaException(TERMINO);
        }

        Usuario usuario = usuarios.findById(sesion.getUsuarioId())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new SesionInvalidaException(TERMINO));

        sesion.rotar(ahora);
        sesiones.guardar(sesion);
        String nuevo = nuevoToken();
        sesiones.guardar(Sesion.nueva(usuario.getId(), hash(nuevo), ahora, duracion));
        return new Renovacion(usuario, nuevo);
    }

    @Override
    @Transactional
    public void cerrar(String token) {
        if (token == null || token.isBlank()) return;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        sesiones.buscarPorTokenHash(hash(token)).ifPresent(s -> {
            s.revocar(ahora);
            sesiones.guardar(s);
        });
    }

    /** Cambió la contraseña (desde el perfil o con el enlace del correo): se cierran todas las sesiones. */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void contrasenaCambiada(ContrasenaCambiada e) {
        sesiones.revocarTodas(e.usuarioId(), LocalDateTime.now(reloj));
    }

    private String nuevoToken() {
        byte[] b = new byte[32];
        azar.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
