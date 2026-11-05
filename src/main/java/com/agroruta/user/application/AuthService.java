package com.agroruta.user.application;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.context.ApplicationEventPublisher;
import com.agroruta.shared.evento.CuentaBloqueada;
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

    public AuthService(RegistrarUsuarioUseCase registrarUsuarioUseCase,
                       BuscarUsuarioUseCase buscarUsuarioUseCase,
                       TokenPort tokenPort,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       VinculoTrabajadorPort vinculoTrabajador,
                       IntentosLogin intentosLogin,
                       ApplicationEventPublisher eventos) {
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.buscarUsuarioUseCase = buscarUsuarioUseCase;
        this.tokenPort = tokenPort;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.vinculoTrabajador = vinculoTrabajador;
        this.intentosLogin = intentosLogin;
        this.eventos = eventos;
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

        Usuario guardado = registrarUsuarioUseCase.registrar(usuario);

        if (rol == Rol.Trabajador) {
            vinculoTrabajador.solicitarAcceso(guardado.getId(), guardado.getNombre(), guardado.getEmail(), cedula);
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