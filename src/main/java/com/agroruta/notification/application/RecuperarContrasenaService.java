package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.RecuperarContrasenaUseCase;
import com.agroruta.notification.domain.RestablecimientoContrasena;
import com.agroruta.notification.domain.RestablecimientoRepository;
import com.agroruta.shared.evento.ContrasenaCambiada;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.user.application.IntentosLogin;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RecuperarContrasenaService implements RecuperarContrasenaUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecuperarContrasenaService.class);

    static final Duration VIGENCIA = Duration.ofMinutes(30);
    /** Máximo de enlaces por cuenta en una hora */
    static final int MAX_POR_HORA = 3;
    static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final UsuarioRepository usuarioRepository;
    private final RestablecimientoRepository restablecimientos;
    private final PasswordEncoder passwordEncoder;
    private final MensajesCorreo mensajes;
    private final EnvioCorreo envio;
    private final IntentosLogin intentosLogin;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    @Autowired
    public RecuperarContrasenaService(UsuarioRepository usuarioRepository, RestablecimientoRepository restablecimientos,
                                      PasswordEncoder passwordEncoder, MensajesCorreo mensajes, EnvioCorreo envio,
                                      IntentosLogin intentosLogin, ApplicationEventPublisher eventos) {
        this(usuarioRepository, restablecimientos, passwordEncoder, mensajes, envio, intentosLogin, eventos,
                Clock.system(ZONA));
    }

    RecuperarContrasenaService(UsuarioRepository usuarioRepository, RestablecimientoRepository restablecimientos,
                               PasswordEncoder passwordEncoder, MensajesCorreo mensajes, EnvioCorreo envio,
                               IntentosLogin intentosLogin, ApplicationEventPublisher eventos, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.restablecimientos = restablecimientos;
        this.passwordEncoder = passwordEncoder;
        this.mensajes = mensajes;
        this.envio = envio;
        this.intentosLogin = intentosLogin;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void solicitar(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).filter(Usuario::isActivo).orElse(null);
        if (usuario == null) {
            log.info("Recuperación pedida para un correo sin cuenta activa");
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (restablecimientos.contarDesde(usuario.getId(), ahora.minusHours(1)) >= MAX_POR_HORA) {
            log.warn("Recuperación: límite de {} enlaces por hora alcanzado para el usuario {}", MAX_POR_HORA, usuario.getId());
            return;
        }
        restablecimientos.anularPendientes(usuario.getId(), ahora);
        String token = nuevoToken();
        restablecimientos.guardar(new RestablecimientoContrasena(null, usuario.getId(), hash(token),
                ahora, ahora.plus(VIGENCIA), null));
        envio.enSegundoPlano(mensajes.recuperarContrasena(usuario.getNombre(), usuario.getEmail(), token,
                VIGENCIA.toMinutes()));
    }

    @Override
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        RestablecimientoContrasena r = token == null || token.isBlank() ? null
                : restablecimientos.buscarPorTokenHash(hash(token.trim())).orElse(null);
        if (r == null || !r.sirve(ahora)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "El enlace no es válido o ya venció. Pide uno nuevo.");
        }
        Usuario usuario = usuarioRepository.findById(r.getUsuarioId()).filter(Usuario::isActivo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OPERATION,
                        "El enlace no es válido o ya venció. Pide uno nuevo."));

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setEmailVerificado(true); // abrir el enlace del correo también demuestra que es suyo
        usuarioRepository.save(usuario);
        r.usar(ahora);
        restablecimientos.guardar(r);
        restablecimientos.anularPendientes(usuario.getId(), ahora);
        intentosLogin.limpiar(usuario.getEmail());
        eventos.publishEvent(new ContrasenaCambiada(usuario.getId()));
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
