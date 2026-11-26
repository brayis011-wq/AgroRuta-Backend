package com.agroruta.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.user.application.ports.in.BuscarUsuarioUseCase;
import com.agroruta.user.application.ports.in.DatosLogin;
import com.agroruta.user.application.ports.in.DatosRegistro;
import com.agroruta.user.application.ports.in.RegistrarUsuarioUseCase;
import com.agroruta.user.application.ports.in.SesionIniciada;
import com.agroruta.user.application.ports.out.TokenPort;
import com.agroruta.user.application.ports.out.VinculoTrabajadorPort;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private IntentosLogin intentosLogin = new IntentosLogin(java.time.Clock.systemUTC());

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventos;

    @Mock
    private com.agroruta.user.application.ports.out.VerificacionCorreoPort verificacion;

    @Mock
    private com.agroruta.user.application.ports.in.SegundoFactorUseCase segundoFactor;

    @InjectMocks
    private AuthService authService;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long ID = 1L;
    private static final String NOMBRE = "Juan Pérez";
    private static final String EMAIL = "juan@agroruta.com";
    private static final String PASSWORD = "secret123";
    private static final String PASSWORD_ENCODED = "$2a$10$hashedpassword";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";
    private static final Rol ROL = Rol.Agricultor;

    private Usuario usuarioGuardado;

    @BeforeEach
    void setUp() {
        usuarioGuardado = new Usuario(ID, NOMBRE, EMAIL, PASSWORD_ENCODED, ROL);
    }

    // ── registrar ─────────────────────────────────────────────────

    @Test
    void deberiaRegistrarUsuarioYRetornarAuthResponse() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, ROL.name(), null);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        SesionIniciada response = authService.registrar(request);

        assertEquals(ID, response.id());
        assertEquals(TOKEN, response.token());
        assertEquals(EMAIL, response.email());
        assertEquals(NOMBRE, response.nombre());
        assertEquals(ROL.name(), response.rol());
    }

    @Test
    void registrarDeberiaEncodearElPasswordAntesDeGuardar() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, ROL.name(), null);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(PASSWORD_ENCODED, captor.getValue().getPassword());
        assertNull(captor.getValue().getId());
    }

    @Test
    void registrarDeberiaPermitirRolTrabajador() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, Rol.Trabajador.name(), "1052334120");

        Usuario usuarioTrabajador = new Usuario(2L, NOMBRE, EMAIL, PASSWORD_ENCODED, Rol.Trabajador);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioTrabajador);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(Rol.Trabajador, captor.getValue().getRol());
        verify(vinculoTrabajador).validarCedulaDisponible("1052334120");
        verify(vinculoTrabajador).solicitarAcceso(2L, NOMBRE, EMAIL, "1052334120");
    }

    @Test
    void registrarTrabajadorSinCedulaDeberiaFallar() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, Rol.Trabajador.name(), null);

        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class, () -> authService.registrar(request));

        assertTrue(ex.getMessage().contains("cédula"));
        verifyNoInteractions(registrarUsuarioUseCase, vinculoTrabajador);
    }

    @Test
    void registrarTrabajadorConCedulaNoRegistradaNoCreaLaCuenta() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, Rol.Trabajador.name(), "999999");
        doThrow(new com.agroruta.shared.exception.BusinessException("Ningún agricultor ha registrado esta cédula."))
                .when(vinculoTrabajador)
                .validarCedulaDisponible("999999");

        assertThrows(com.agroruta.shared.exception.BusinessException.class, () -> authService.registrar(request));

        verifyNoInteractions(registrarUsuarioUseCase);
    }

    @Test
    void registrarAgricultorNoUsaLaCedula() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, Rol.Agricultor.name(), null);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.registrar(request);

        verifyNoInteractions(vinculoTrabajador);
    }

    @Test
    void registrarDeberiaAsignarAgricultorSiNoSeIndicaRol() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, null, null);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(Rol.Agricultor, captor.getValue().getRol());
    }

    @Test
    void registrarDeberiaRechazarRolAdministrador() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, Rol.Administrador.name(), null);

        assertThrows(DatoInvalidoException.class, () -> authService.registrar(request));
        verify(registrarUsuarioUseCase, never()).registrar(any(Usuario.class));
    }

    @Test
    void registrarDeberiaRechazarRolInexistente() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, "SuperUsuario", null);

        assertThrows(DatoInvalidoException.class, () -> authService.registrar(request));
        verify(registrarUsuarioUseCase, never()).registrar(any(Usuario.class));
    }

    @Test
    void registrarDeberiaDelegarLaGeneracionDelToken() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, ROL.name(), null);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.registrar(request);

        verify(tokenPort, times(1)).generarToken(any(Usuario.class));
    }

    @Test
    void registrarDeberiaLlamarAlUsoCasoConLosDatosDelRequest() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, ROL.name(), null);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(NOMBRE, captor.getValue().getNombre());
        assertEquals(EMAIL, captor.getValue().getEmail());
        assertEquals(PASSWORD_ENCODED, captor.getValue().getPassword());
        assertEquals(ROL, captor.getValue().getRol());
    }

    @Test
    void registrarDeberiaCubrirLosRolesPermitidos() {
        for (Rol rol : new Rol[] {Rol.Agricultor, Rol.Trabajador}) {
            DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, rol.name(), "1052334120");

            Usuario usuario = new Usuario(ID, NOMBRE, EMAIL, PASSWORD_ENCODED, rol);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
            when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuario);
            when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

            SesionIniciada response = authService.registrar(request);

            assertEquals(rol.name(), response.rol());
        }
    }

    // ── login ─────────────────────────────────────────────────────

    @Test
    void deberiaLoginYRetornarAuthResponse() {
        DatosLogin request = new DatosLogin(EMAIL, PASSWORD, null, null);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        SesionIniciada response = authService.login(request);

        assertEquals(ID, response.id());
        assertEquals(TOKEN, response.token());
        assertEquals(EMAIL, response.email());
        assertEquals(NOMBRE, response.nombre());
        assertEquals(ROL.name(), response.rol());
    }

    @Test
    void loginDeberiaAutenticarConEmailYPasswordCorrectos() {
        DatosLogin request = new DatosLogin(EMAIL, PASSWORD, null, null);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals(EMAIL, captor.getValue().getPrincipal());
        assertEquals(PASSWORD, captor.getValue().getCredentials());
    }

    @Test
    void loginDeberiaBuscarElUsuarioPorEmailTrasAutenticar() {
        DatosLogin request = new DatosLogin(EMAIL, PASSWORD, null, null);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.login(request);

        InOrder orden = inOrder(authenticationManager, buscarUsuarioUseCase);
        orden.verify(authenticationManager).authenticate(any());
        orden.verify(buscarUsuarioUseCase).buscarPorEmail(EMAIL);
    }

    @Test
    void loginDeberiaDelegarLaGeneracionDelToken() {
        DatosLogin request = new DatosLogin(EMAIL, PASSWORD, null, null);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        authService.login(request);

        verify(tokenPort, times(1)).generarToken(any(Usuario.class));
    }

    @Test
    void loginDeberiaLanzarExcepcionSiLaAutenticacionFalla() {
        DatosLogin request = new DatosLogin(EMAIL, "wrongpassword", null, null);

        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Bad credentials") {});

        assertThrows(org.springframework.security.core.AuthenticationException.class, () -> authService.login(request));

        verifyNoInteractions(buscarUsuarioUseCase);
        verifyNoInteractions(tokenPort);
    }

    // ── límite de intentos ────────────────────────────────────────

    @Test
    void loginDeberiaBloquearElCorreoTrasDiezContrasenasEquivocadas() {
        DatosLogin request = new DatosLogin(EMAIL, "wrongpassword", null, null);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        for (int i = 0; i < IntentosLogin.MAX_FALLOS; i++) {
            assertThrows(
                    org.springframework.security.authentication.BadCredentialsException.class,
                    () -> authService.login(request));
        }

        com.agroruta.shared.exception.BusinessException e =
                assertThrows(com.agroruta.shared.exception.BusinessException.class, () -> authService.login(request));
        org.junit.jupiter.api.Assertions.assertEquals(
                com.agroruta.shared.exception.ErrorCode.TOO_MANY_ATTEMPTS, e.getErrorCode());
        verify(authenticationManager, org.mockito.Mockito.times(IntentosLogin.MAX_FALLOS))
                .authenticate(any());
        verify(eventos).publishEvent(new com.agroruta.shared.evento.CuentaBloqueada(EMAIL, 15));
    }

    @Test
    void loginCorrectoDeberiaOlvidarLosFallosAnteriores() {
        DatosLogin request = new DatosLogin(EMAIL, PASSWORD, null, null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any())).thenReturn(TOKEN);

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
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, ROL.name(), null);
        when(verificacion.activa()).thenReturn(true);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        SesionIniciada response = authService.registrar(request);

        org.junit.jupiter.api.Assertions.assertNull(response.token());
        org.junit.jupiter.api.Assertions.assertTrue(response.verificacionPendiente());
        org.mockito.ArgumentCaptor<Usuario> u = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(verificacion).enviarEnlace(u.capture());
        org.junit.jupiter.api.Assertions.assertFalse(u.getValue().isEmailVerificado());
        verifyNoInteractions(tokenPort);
    }

    @Test
    void registrarSinVerificacionDeberiaQuedarConfirmado() {
        DatosRegistro request = new DatosRegistro(NOMBRE, EMAIL, PASSWORD, ROL.name(), null);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        SesionIniciada response = authService.registrar(request);

        assertEquals(TOKEN, response.token());
        org.junit.jupiter.api.Assertions.assertFalse(response.verificacionPendiente());
        verify(verificacion, never()).enviarEnlace(any());
    }

    @Test
    void loginConCorreoSinConfirmarDeberiaRechazarseDespuesDeLaContrasena() {
        DatosLogin request = new DatosLogin(EMAIL, PASSWORD, null, null);
        usuarioGuardado.setEmailVerificado(false);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(verificacion.activa()).thenReturn(true);

        com.agroruta.shared.exception.BusinessException e =
                assertThrows(com.agroruta.shared.exception.BusinessException.class, () -> authService.login(request));
        assertEquals(com.agroruta.shared.exception.ErrorCode.EMAIL_NOT_VERIFIED, e.getErrorCode());
        verify(authenticationManager).authenticate(any());
        verifyNoInteractions(tokenPort);
    }

    @Test
    void verificarCorreoDeberiaIniciarSesion() {
        when(verificacion.confirmar("tok")).thenReturn(usuarioGuardado);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        SesionIniciada response = authService.verificarCorreo("tok");

        assertEquals(TOKEN, response.token());
        assertEquals(EMAIL, response.email());
    }

    // ── segundo factor ────────────────────────────────────────────

    private DatosLogin loginConCodigo(String codigo) {
        DatosLogin request = new DatosLogin(EMAIL, PASSWORD, codigo, null);
        when(buscarUsuarioUseCase.buscarPorEmail(EMAIL)).thenReturn(usuarioGuardado);
        when(segundoFactor.activo(ID)).thenReturn(true);
        return request;
    }

    @Test
    void loginConSegundoFactorSinCodigoDeberiaPedirlo() {
        DatosLogin request = loginConCodigo(null);

        com.agroruta.shared.exception.BusinessException e =
                assertThrows(com.agroruta.shared.exception.BusinessException.class, () -> authService.login(request));

        assertEquals(com.agroruta.shared.exception.ErrorCode.TWO_FACTOR_REQUIRED, e.getErrorCode());
        verify(segundoFactor, never()).verificar(any(), any());
        verifyNoInteractions(tokenPort);
    }

    @Test
    void loginConCodigoEquivocadoDeberiaRechazarseYContarComoIntento() {
        DatosLogin request = loginConCodigo("000000");
        when(segundoFactor.verificar(ID, "000000")).thenReturn(false);

        for (int i = 0; i < IntentosLogin.MAX_FALLOS; i++) {
            com.agroruta.shared.exception.BusinessException e = assertThrows(
                    com.agroruta.shared.exception.BusinessException.class, () -> authService.login(request));
            assertEquals(com.agroruta.shared.exception.ErrorCode.TWO_FACTOR_INVALID, e.getErrorCode());
        }
        // Después de diez códigos equivocados la cuenta queda bloqueada, aunque la contraseña sea buena
        com.agroruta.shared.exception.BusinessException bloqueo =
                assertThrows(com.agroruta.shared.exception.BusinessException.class, () -> authService.login(request));
        assertEquals(com.agroruta.shared.exception.ErrorCode.TOO_MANY_ATTEMPTS, bloqueo.getErrorCode());
        verifyNoInteractions(tokenPort);
    }

    @Test
    void loginConCodigoCorrectoDeberiaEntrar() {
        DatosLogin request = loginConCodigo("123456");
        when(segundoFactor.verificar(ID, "123456")).thenReturn(true);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        assertEquals(TOKEN, authService.login(request).token());
    }

    @Test
    void conDispositivoRecordadoNoDeberiaPedirElCodigo() {
        DatosLogin request = loginConCodigo(null);
        when(segundoFactor.dispositivoRecordado(ID, "marca-valida")).thenReturn(true);
        when(tokenPort.generarToken(any(Usuario.class))).thenReturn(TOKEN);

        assertEquals(TOKEN, authService.login(request, "marca-valida").token());
        verify(segundoFactor, never()).verificar(any(), any());
    }

    @Test
    void conMarcaDeDispositivoInvalidaDeberiaPedirElCodigo() {
        DatosLogin request = loginConCodigo(null);
        when(segundoFactor.dispositivoRecordado(ID, "marca-falsa")).thenReturn(false);

        com.agroruta.shared.exception.BusinessException e = assertThrows(
                com.agroruta.shared.exception.BusinessException.class, () -> authService.login(request, "marca-falsa"));

        assertEquals(com.agroruta.shared.exception.ErrorCode.TWO_FACTOR_REQUIRED, e.getErrorCode());
        verifyNoInteractions(tokenPort);
    }

    @Test
    void laMarcaDeDispositivoNoSirveSinLaContrasena() {
        DatosLogin request = new DatosLogin(EMAIL, "otra", null, null);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("mala"));

        assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(request, "marca-valida"));
        verifyNoInteractions(segundoFactor, tokenPort);
    }

    @Test
    void reenviarVerificacionDeberiaNormalizarElCorreo() {
        authService.reenviarVerificacion("  Juan@AgroRuta.com ");
        verify(verificacion).reenviar("juan@agroruta.com");
    }
}
