package com.agroruta.user.application;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.context.ApplicationEventPublisher;
import com.agroruta.shared.evento.CuentaBloqueada;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.user.application.ports.out.VerificacionCorreoPort;
import org.springframework.transaction.annotation.Transactional;
import com.agroruta.user.application.ports.out.VinculoTrabajadorPort;
import com.agroruta.user.application.ports.in.BuscarUsuarioUseCase;
import com.agroruta.user.application.ports.in.RegistrarUsuarioUseCase;
import com.agroruta.user.application.ports.out.TokenPort;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import com.agroruta.user.infrastructure.web.dto.AuthResponse;
import com.agroruta.user.infrastructure.web.dto.LoginRequest;
import com.agroruta.user.infrastructure.web.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class AuthService {

    /** Roles que una persona puede elegir al auto-registrarse. Administrador NUNCA. */
    private static final Set<Rol> ROLES_AUTOREGISTRO = EnumSet.of(Rol.Agricultor, Rol.Trabajador);

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final BuscarUsuarioUseCase buscarUsuarioUseCase;
    private final TokenPort tokenPort;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final VinculoTrabajadorPort vinculoTrabajador;
    private final IntentosLogin intentosLogin;
    private final ApplicationEventPublisher eventos;
    private final VerificacionCorreoPort verificacion;

    public AuthService(RegistrarUsuarioUseCase registrarUsuarioUseCase,
                       BuscarUsuarioUseCase buscarUsuarioUseCase,
                       TokenPort tokenPort,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       VinculoTrabajadorPort vinculoTrabajador,
                       IntentosLogin intentosLogin,
                       ApplicationEventPublisher eventos,
                       VerificacionCorreoPort verificacion) {
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.buscarUsuarioUseCase = buscarUsuarioUseCase;
        this.tokenPort = tokenPort;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.vinculoTrabajador = vinculoTrabajador;
        this.intentosLogin = intentosLogin;
        this.eventos = eventos;
        this.verificacion = verificacion;
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        Rol rol = resolverRolAutoRegistro(request.getRol());

        // Un trabajador se registra con su cédula: debe haberlo registrado antes un agricultor
        String cedula = request.getCedula() == null ? "" : request.getCedula().trim();
        if (rol == Rol.Trabajador) {
            if (cedula.isEmpty()) {
                throw new IllegalArgumentException("La cédula es obligatoria para registrarse como trabajador.");
            }
            vinculoTrabajador.validarCedulaDisponible(cedula);
        }

        Usuario usuario = new Usuario(
                null,
                request.getNombre(),
                normalizarEmail(request.getEmail()),
                passwordEncoder.encode(request.getPassword()),
                rol
        );
        boolean verificar = verificacion.activa();
        usuario.setEmailVerificado(!verificar);

        Usuario guardado = registrarUsuarioUseCase.registrar(usuario);

        if (rol == Rol.Trabajador) {
            vinculoTrabajador.solicitarAcceso(guardado.getId(), guardado.getNombre(), guardado.getEmail(), cedula);
        }
        if (verificar) {
            // Sin sesión hasta que confirme el correo con el enlace
            verificacion.enviarEnlace(guardado);
            return new AuthResponse(guardado.getId(), null, guardado.getEmail(), guardado.getNombre(),
                    guardado.getRol().name(), true);
        }
        CustomUserDetails userDetails = new CustomUserDetails(guardado);
        String token = tokenPort.generateToken(userDetails);

        return new AuthResponse(
                guardado.getId(),
                token,
                guardado.getEmail(),
                guardado.getNombre(),
                guardado.getRol().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        intentosLogin.exigirNoBloqueado(request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            if (intentosLogin.registrarFallo(request.getEmail())) {
                eventos.publishEvent(new CuentaBloqueada(request.getEmail(), IntentosLogin.BLOQUEO.toMinutes()));
            }
            throw e;
        }
        intentosLogin.limpiar(request.getEmail());

        Usuario usuario = buscarUsuarioUseCase.buscarPorEmail(request.getEmail());
        // Se revisa después de la contraseña, para no contarle a cualquiera qué cuentas faltan por confirmar
        if (!usuario.isEmailVerificado() && verificacion.activa()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED,
                    "Confirma tu correo para entrar: abre el enlace que te enviamos a " + usuario.getEmail() + ".");
        }
        return respuestaDeSesion(usuario);
    }

    /** El enlace del correo de confirmación: confirma la cuenta y deja la sesión iniciada. */
    public AuthResponse verificarCorreo(String token) {
        return respuestaDeSesion(verificacion.confirmar(token));
    }

    /** Vuelve a mandar el enlace de confirmación. Responde igual exista o no la cuenta. */
    public void reenviarVerificacion(String email) {
        verificacion.reenviar(normalizarEmail(email));
    }

    /** Respuesta con un token de acceso nuevo para el usuario (login, confirmación o renovación). */
    public AuthResponse respuestaDeSesion(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        String token = tokenPort.generateToken(userDetails);
        return new AuthResponse(
                usuario.getId(),
                token,
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol().name()
        );
    }

    /** Los correos se guardan en minúscula y sin espacios: "Ana@X.com " y "ana@x.com" son la misma cuenta. */
    static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private Rol resolverRolAutoRegistro(String rolSolicitado) {
        if (rolSolicitado == null || rolSolicitado.isBlank()) {
            return Rol.Agricultor;
        }
        Rol rol;
        try {
            rol = Rol.valueOf(rolSolicitado.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol no válido. Opciones: Agricultor, Trabajador");
        }
        if (!ROLES_AUTOREGISTRO.contains(rol)) {
            throw new IllegalArgumentException("No es posible registrarse con el rol " + rol.name());
        }
        return rol;
    }
}