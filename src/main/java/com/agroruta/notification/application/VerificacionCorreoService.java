package com.agroruta.notification.application;

import com.agroruta.notification.domain.VerificacionCorreo;
import com.agroruta.notification.application.ports.out.VerificacionCorreoRepository;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.user.application.ports.out.VerificacionCorreoPort;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Confirmación del correo al registrarse: manda un enlace de un solo uso que vence en 24 horas.
 * Mientras no lo abra, la cuenta no puede iniciar sesión.
 */
@Service
public class VerificacionCorreoService implements VerificacionCorreoPort {

    private static final Logger log = LoggerFactory.getLogger(VerificacionCorreoService.class);

    static final Duration VIGENCIA = Duration.ofHours(24);
    static final int MAX_POR_HORA = 3;
    private static final String NO_SIRVE = "El enlace no es válido o ya venció. Pide uno nuevo.";

    private final boolean activa;
    private final UsuarioRepository usuarios;
    private final VerificacionCorreoRepository verificaciones;
    private final MensajesCorreo mensajes;
    private final EnvioCorreo envio;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    @Autowired
    public VerificacionCorreoService(@Value("${correo.verificar-registro:true}") boolean activa,
                                     UsuarioRepository usuarios, VerificacionCorreoRepository verificaciones,
                                     MensajesCorreo mensajes, EnvioCorreo envio) {
        this(activa, usuarios, verificaciones, mensajes, envio, Clock.system(RecuperarContrasenaService.ZONA));
    }

    VerificacionCorreoService(boolean activa, UsuarioRepository usuarios, VerificacionCorreoRepository verificaciones,
                              MensajesCorreo mensajes, EnvioCorreo envio, Clock reloj) {
        this.activa = activa;
        this.usuarios = usuarios;
        this.verificaciones = verificaciones;
        this.mensajes = mensajes;
        this.envio = envio;
        this.reloj = reloj;
    }

    @Override
    public boolean activa() {
        return activa;
    }

    @Override
    @Transactional
    public void enviarEnlace(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        verificaciones.anularPendientes(usuario.getId(), ahora);
        byte[] b = new byte[32];
        azar.nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        verificaciones.guardar(new VerificacionCorreo(null, usuario.getId(), RecuperarContrasenaService.hash(token),
                ahora, ahora.plus(VIGENCIA), null));
        envio.enSegundoPlano(mensajes.verificarCorreo(usuario.getNombre(), usuario.getEmail(), token, VIGENCIA.toHours()));
    }

    @Override
    @Transactional
    public Usuario confirmar(String token) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        VerificacionCorreo v = token == null || token.isBlank() ? null
                : verificaciones.buscarPorTokenHash(RecuperarContrasenaService.hash(token.trim())).orElse(null);
        if (v == null || !v.sirve(ahora)) throw new BusinessException(ErrorCode.INVALID_OPERATION, NO_SIRVE);
        Usuario usuario = usuarios.findById(v.getUsuarioId()).filter(Usuario::isActivo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OPERATION, NO_SIRVE));
        v.usar(ahora);
        verificaciones.guardar(v);
        verificaciones.anularPendientes(usuario.getId(), ahora);
        if (!usuario.isEmailVerificado()) {
            usuario.setEmailVerificado(true);
            usuario = usuarios.save(usuario);
        }
        return usuario;
    }

    @Override
    @Transactional
    public void reenviar(String email) {
        Usuario usuario = usuarios.findByEmail(email).filter(Usuario::isActivo).orElse(null);
        if (usuario == null || usuario.isEmailVerificado()) return;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (verificaciones.contarDesde(usuario.getId(), ahora.minusHours(1)) >= MAX_POR_HORA) {
            log.warn("Verificación: límite de {} enlaces por hora para el usuario {}", MAX_POR_HORA, usuario.getId());
            return;
        }
        enviarEnlace(usuario);
    }
}
