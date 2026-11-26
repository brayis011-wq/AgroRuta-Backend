package com.agroruta.user.application;

import com.agroruta.shared.evento.CuentaBloqueada;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.user.application.ports.in.BuscarUsuarioUseCase;
import com.agroruta.user.application.ports.in.DatosLogin;
import com.agroruta.user.application.ports.in.DatosRegistro;
import com.agroruta.user.application.ports.in.RegistrarUsuarioUseCase;
import com.agroruta.user.application.ports.in.SesionIniciada;
import com.agroruta.user.application.ports.out.TokenPort;
import com.agroruta.user.application.ports.out.VerificacionCorreoPort;
import com.agroruta.user.application.ports.out.VinculoTrabajadorPort;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final com.agroruta.user.application.ports.in.SegundoFactorUseCase segundoFactor;

    public AuthService(
            RegistrarUsuarioUseCase registrarUsuarioUseCase,
            BuscarUsuarioUseCase buscarUsuarioUseCase,
            TokenPort tokenPort,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            VinculoTrabajadorPort vinculoTrabajador,
            IntentosLogin intentosLogin,
            ApplicationEventPublisher eventos,
            VerificacionCorreoPort verificacion,
            com.agroruta.user.application.ports.in.SegundoFactorUseCase segundoFactor) {
        this.segundoFactor = segundoFactor;
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
    public SesionIniciada registrar(DatosRegistro request) {
        Rol rol = resolverRolAutoRegistro(request.rol());

        // Un trabajador se registra con su cédula: debe haberlo registrado antes un agricultor
        String cedula = request.cedula() == null ? "" : request.cedula().trim();
        if (rol == Rol.Trabajador) {
            if (cedula.isEmpty()) {
                throw new DatoInvalidoException("La cédula es obligatoria para registrarse como trabajador.");
            }
            vinculoTrabajador.validarCedulaDisponible(cedula);
        }

        Usuario usuario = new Usuario(
                null,
                request.nombre(),
                normalizarEmail(request.email()),
                passwordEncoder.encode(request.password()),
                rol);
        boolean verificar = verificacion.activa();
        usuario.setEmailVerificado(!verificar);

        Usuario guardado = registrarUsuarioUseCase.registrar(usuario);

        if (rol == Rol.Trabajador) {
            vinculoTrabajador.solicitarAcceso(guardado.getId(), guardado.getNombre(), guardado.getEmail(), cedula);
        }
        if (verificar) {
            // Sin sesión hasta que confirme el correo con el enlace
            verificacion.enviarEnlace(guardado);
            return new SesionIniciada(
                    guardado.getId(),
                    null,
                    guardado.getEmail(),
                    guardado.getNombre(),
                    guardado.getRol().name(),
                    true);
        }
        String token = tokenPort.generarToken(guardado);

        return new SesionIniciada(
                guardado.getId(),
                token,
                guardado.getEmail(),
                guardado.getNombre(),
                guardado.getRol().name());
    }

    public SesionIniciada login(DatosLogin request) {
        return login(request, null);
    }

    /**
     * @param dispositivo marca de "dispositivo recordado" que trae el navegador (cookie), o null.
     *                    Si es válida, no se pide el código de la verificación en dos pasos.
     */
    public SesionIniciada login(DatosLogin request, String dispositivo) {
        intentosLogin.exigirNoBloqueado(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            if (intentosLogin.registrarFallo(request.email())) {
                eventos.publishEvent(new CuentaBloqueada(request.email(), IntentosLogin.BLOQUEO.toMinutes()));
            }
            throw e;
        }
        Usuario usuario = buscarUsuarioUseCase.buscarPorEmail(request.email());
        exigirSegundoFactor(usuario, request, dispositivo);
        // Solo cuando pasó todo: un código equivocado cuenta como intento fallido
        intentosLogin.limpiar(request.email());

        // Se revisa después de la contraseña, para no contarle a cualquiera qué cuentas faltan por confirmar
        if (!usuario.isEmailVerificado() && verificacion.activa()) {
            throw new BusinessException(
                    ErrorCode.EMAIL_NOT_VERIFIED,
                    "Confirma tu correo para entrar: abre el enlace que te enviamos a " + usuario.getEmail() + ".");
        }
        return respuestaDeSesion(usuario);
    }

    /**
     * Con verificación en dos pasos, la contraseña sola no alcanza. Sin código se responde
     * TWO_FACTOR_REQUIRED (la pantalla muestra el campo); con un código equivocado, TWO_FACTOR_INVALID,
     * y cuenta para el bloqueo por intentos, para que nadie pruebe códigos al azar.
     */
    private void exigirSegundoFactor(Usuario usuario, DatosLogin request, String dispositivo) {
        if (!segundoFactor.activo(usuario.getId())) return;
        // Este navegador ya escribió el código hace menos de un mes: basta con la contraseña
        if (segundoFactor.dispositivoRecordado(usuario.getId(), dispositivo)) return;
        String codigo = request.codigo();
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException(
                    ErrorCode.TWO_FACTOR_REQUIRED, "Escribe el código de 6 dígitos de tu app de autenticación.");
        }
        if (!segundoFactor.verificar(usuario.getId(), codigo)) {
            if (intentosLogin.registrarFallo(request.email())) {
                eventos.publishEvent(new CuentaBloqueada(request.email(), IntentosLogin.BLOQUEO.toMinutes()));
            }
            throw new BusinessException(
                    ErrorCode.TWO_FACTOR_INVALID,
                    "El código no es correcto. Usa el más reciente de tu app o un código de respaldo.");
        }
    }

    /** Marca para no volver a pedir el código en este dispositivo; vacía si la cuenta no tiene segundo factor. */
    public java.util.Optional<String> recordarDispositivo(Long usuarioId) {
        return segundoFactor.recordarDispositivo(usuarioId);
    }

    /** El enlace del correo de confirmación: confirma la cuenta y deja la sesión iniciada. */
    public SesionIniciada verificarCorreo(String token) {
        return respuestaDeSesion(verificacion.confirmar(token));
    }

    /** Vuelve a mandar el enlace de confirmación. Responde igual exista o no la cuenta. */
    public void reenviarVerificacion(String email) {
        verificacion.reenviar(normalizarEmail(email));
    }

    /** Respuesta con un token de acceso nuevo para el usuario (login, confirmación o renovación). */
    public SesionIniciada respuestaDeSesion(Usuario usuario) {
        String token = tokenPort.generarToken(usuario);
        return new SesionIniciada(
                usuario.getId(),
                token,
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol().name());
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
            throw new DatoInvalidoException("Rol no válido. Opciones: Agricultor, Trabajador");
        }
        if (!ROLES_AUTOREGISTRO.contains(rol)) {
            throw new DatoInvalidoException("No es posible registrarse con el rol " + rol.name());
        }
        return rol;
    }
}
