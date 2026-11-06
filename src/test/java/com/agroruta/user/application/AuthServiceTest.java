package com.agroruta.user.application;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.InOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;

    @Mock
    private BuscarUsuarioUseCase buscarUsuarioUseCase;

    @Mock
    private TokenPort tokenPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private VinculoTrabajadorPort vinculoTrabajador;

    @org.mockito.Spy
    private IntentosLogin intentosLogin = new IntentosLogin();

    @Mock private org.springframework.context.ApplicationEventPublisher eventos;

    @Mock private com.agroruta.user.application.ports.out.VerificacionCorreoPort verificacion;

    @InjectMocks
    private AuthService authService;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long   ID       = 1L;
    private static final String NOMBRE   = "Juan Pérez";
    private static final String EMAIL    = "juan@agroruta.com";
    private static final String PASSWORD = "secret123";
    private static final String PASSWORD_ENCODED = "$2a$10$hashedpassword";
    private static final String TOKEN    = "eyJhbGciOiJIUzI1NiJ9.test.token";
    private static final Rol    ROL      = Rol.Agricultor;

    private Usuario usuarioGuardado;

    @BeforeEach
    void setUp() {
        usuarioGuardado = new Usuario(ID, NOMBRE, EMAIL, PASSWORD_ENCODED, ROL);
    }

    // ── registrar ─────────────────────────────────────────────────

    @Test
    void deberiaRegistrarUsuarioYRetornarAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(ROL.name());

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        AuthResponse response = authService.registrar(request);

        assertEquals(ID,       response.getId());
        assertEquals(TOKEN,    response.getToken());
        assertEquals(EMAIL,    response.getEmail());
        assertEquals(NOMBRE,   response.getNombre());
        assertEquals(ROL.name(), response.getRol());
    }

    @Test
    void registrarDeberiaEncodearElPasswordAntesDeGuardar() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(ROL.name());

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(PASSWORD_ENCODED, captor.getValue().getPassword());
        assertNull(captor.getValue().getId());
    }

    @Test
    void registrarDeberiaPermitirRolTrabajador() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(Rol.Trabajador.name());
        request.setCedula("1052334120");

        Usuario usuarioTrabajador = new Usuario(2L, NOMBRE, EMAIL, PASSWORD_ENCODED, Rol.Trabajador);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioTrabajador);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(Rol.Trabajador, captor.getValue().getRol());
        verify(vinculoTrabajador).validarCedulaDisponible("1052334120");
        verify(vinculoTrabajador).solicitarAcceso(2L, NOMBRE, EMAIL, "1052334120");
    }

    @Test
    void registrarTrabajadorSinCedulaDeberiaFallar() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(Rol.Trabajador.name());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registrar(request));

        assertTrue(ex.getMessage().contains("cédula"));
        verifyNoInteractions(registrarUsuarioUseCase, vinculoTrabajador);
    }

    @Test
    void registrarTrabajadorConCedulaNoRegistradaNoCreaLaCuenta() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(Rol.Trabajador.name());
        request.setCedula("999999");
        doThrow(new com.agroruta.shared.exception.BusinessException("Ningún agricultor ha registrado esta cédula."))
                .when(vinculoTrabajador).validarCedulaDisponible("999999");

        assertThrows(com.agroruta.shared.exception.BusinessException.class, () -> authService.registrar(request));

        verifyNoInteractions(registrarUsuarioUseCase);
    }

    @Test
    void registrarAgricultorNoUsaLaCedula() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(Rol.Agricultor.name());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.registrar(request);

        verifyNoInteractions(vinculoTrabajador);
    }

    @Test
    void registrarDeberiaAsignarAgricultorSiNoSeIndicaRol() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(null);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(Rol.Agricultor, captor.getValue().getRol());
    }

    @Test
    void registrarDeberiaRechazarRolAdministrador() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(Rol.Administrador.name());

        assertThrows(IllegalArgumentException.class, () -> authService.registrar(request));
        verify(registrarUsuarioUseCase, never()).registrar(any(Usuario.class));
    }

    @Test
    void registrarDeberiaRechazarRolInexistente() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol("SuperUsuario");

        assertThrows(IllegalArgumentException.class, () -> authService.registrar(request));
        verify(registrarUsuarioUseCase, never()).registrar(any(Usuario.class));
    }

    @Test
    void registrarDeberiaDelegarLaGeneracionDelToken() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(ROL.name());

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.registrar(request);

        verify(tokenPort, times(1)).generateToken(any(CustomUserDetails.class));
    }

    @Test
    void registrarDeberiaLlamarAlUsoCasoConLosDatosDelRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(ROL.name());

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(NOMBRE,          captor.getValue().getNombre());
        assertEquals(EMAIL,           captor.getValue().getEmail());
        assertEquals(PASSWORD_ENCODED, captor.getValue().getPassword());
        assertEquals(ROL,             captor.getValue().getRol());
    }

    @Test
    void registrarDeberiaCubrirLosRolesPermitidos() {
        for (Rol rol : new Rol[]{Rol.Agricultor, Rol.Trabajador}) {
            RegisterRequest request = new RegisterRequest();
            request.setNombre(NOMBRE);
            request.setEmail(EMAIL);
            request.setPassword(PASSWORD);
            request.setRol(rol.name());
            request.setCedula("1052334120");

            Usuario usuario = new Usuario(ID, NOMBRE, EMAIL, PASSWORD_ENCODED, rol);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
            when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuario);
            when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

            AuthResponse response = authService.registrar(request);

            assertEquals(rol.name(), response.getRol());
        }
    }

    // ── login ─────────────────────────────────────────────────────

    @Test
    void deberiaLoginYRetornarAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        AuthResponse response = authService.login(request);

        assertEquals(ID,         response.getId());
        assertEquals(TOKEN,      response.getToken());
        assertEquals(EMAIL,      response.getEmail());
        assertEquals(NOMBRE,     response.getNombre());
        assertEquals(ROL.name(), response.getRol());
    }

    @Test
    void loginDeberiaAutenticarConEmailYPasswordCorrectos() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals(EMAIL,    captor.getValue().getPrincipal());
        assertEquals(PASSWORD, captor.getValue().getCredentials());
    }

    @Test
    void loginDeberiaBuscarElUsuarioPorEmailTrasAutenticar() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.login(request);

        InOrder orden = inOrder(authenticationManager, buscarUsuarioUseCase);
        orden.verify(authenticationManager).authenticate(any());
        orden.verify(buscarUsuarioUseCase).buscarPorEmail(EMAIL);
    }

    @Test
    void loginDeberiaDelegarLaGeneracionDelToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        authService.login(request);

        verify(tokenPort, times(1)).generateToken(any(CustomUserDetails.class));
    }

    @Test
    void loginDeberiaLanzarExcepcionSiLaAutenticacionFalla() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Bad credentials") {});

        assertThrows(
                org.springframework.security.core.AuthenticationException.class,
                () -> authService.login(request)
        );

        verifyNoInteractions(buscarUsuarioUseCase);
        verifyNoInteractions(tokenPort);
    }

    // ── límite de intentos ────────────────────────────────────────

    @Test
    void loginDeberiaBloquearElCorreoTrasDiezContrasenasEquivocadas() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword("wrongpassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        for (int i = 0; i < IntentosLogin.MAX_FALLOS; i++) {
            assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                    () -> authService.login(request));
        }

        com.agroruta.shared.exception.BusinessException e = assertThrows(
                com.agroruta.shared.exception.BusinessException.class, () -> authService.login(request));
        org.junit.jupiter.api.Assertions.assertEquals(
                com.agroruta.shared.exception.ErrorCode.TOO_MANY_ATTEMPTS, e.getErrorCode());
        verify(authenticationManager, org.mockito.Mockito.times(IntentosLogin.MAX_FALLOS)).authenticate(any());
        verify(eventos).publishEvent(new com.agroruta.shared.evento.CuentaBloqueada(EMAIL, 15));
    }

    @Test
    void loginCorrectoDeberiaOlvidarLosFallosAnteriores() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any())).thenReturn(TOKEN);

        authService.login(request);

        verify(intentosLogin).limpiar(EMAIL);
    }

    @Test
    void normalizarEmailDeberiaQuitarEspaciosYMayusculas() {
        org.junit.jupiter.api.Assertions.assertEquals("ana@x.com", AuthService.normalizarEmail("  Ana@X.com "));
        org.junit.jupiter.api.Assertions.assertNull(AuthService.normalizarEmail(null));
    }

    // ── confirmación del correo ───────────────────────────────────

    @Test
    void registrarConVerificacionNoDeberiaDarSesionYDeberiaMandarElEnlace() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(ROL.name());
        when(verificacion.activa()).thenReturn(true);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.registrar(request);

        org.junit.jupiter.api.Assertions.assertNull(response.getToken());
        org.junit.jupiter.api.Assertions.assertTrue(response.isVerificacionPendiente());
        org.mockito.ArgumentCaptor<Usuario> u = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(verificacion).enviarEnlace(u.capture());
        org.junit.jupiter.api.Assertions.assertFalse(u.getValue().isEmailVerificado());
        verifyNoInteractions(tokenPort);
    }

    @Test
    void registrarSinVerificacionDeberiaQuedarConfirmado() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(ROL.name());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        AuthResponse response = authService.registrar(request);

        assertEquals(TOKEN, response.getToken());
        org.junit.jupiter.api.Assertions.assertFalse(response.isVerificacionPendiente());
        verify(verificacion, never()).enviarEnlace(any());
    }

    @Test
    void loginConCorreoSinConfirmarDeberiaRechazarseDespuesDeLaContrasena() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        usuarioGuardado.setEmailVerificado(false);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(verificacion.activa()).thenReturn(true);

        com.agroruta.shared.exception.BusinessException e = assertThrows(
                com.agroruta.shared.exception.BusinessException.class, () -> authService.login(request));
        assertEquals(com.agroruta.shared.exception.ErrorCode.EMAIL_NOT_VERIFIED, e.getErrorCode());
        verify(authenticationManager).authenticate(any());
        verifyNoInteractions(tokenPort);
    }

    @Test
    void verificarCorreoDeberiaIniciarSesion() {
        when(verificacion.confirmar("tok")).thenReturn(usuarioGuardado);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        AuthResponse response = authService.verificarCorreo("tok");

        assertEquals(TOKEN, response.getToken());
        assertEquals(EMAIL, response.getEmail());
    }

    @Test
    void reenviarVerificacionDeberiaNormalizarElCorreo() {
        authService.reenviarVerificacion("  Juan@AgroRuta.com ");
        verify(verificacion).reenviar("juan@agroruta.com");
    }
}
