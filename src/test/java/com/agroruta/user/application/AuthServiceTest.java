package com.agroruta.user.application;

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
    void registrarDeberiaConvertirElRolCorrectamente() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre(NOMBRE);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setRol(Rol.Administrador.name());

        Usuario usuarioAdmin = new Usuario(2L, NOMBRE, EMAIL, PASSWORD_ENCODED, Rol.Administrador);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_ENCODED);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(usuarioAdmin);
        when(tokenPort.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

        AuthResponse response = authService.registrar(request);

        assertEquals(Rol.Administrador.name(), response.getRol());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(registrarUsuarioUseCase).registrar(captor.capture());
        assertEquals(Rol.Administrador, captor.getValue().getRol());
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
    void registrarDeberiaCubrirTodosLosRoles() {
        for (Rol rol : Rol.values()) {
            RegisterRequest request = new RegisterRequest();
            request.setNombre(NOMBRE);
            request.setEmail(EMAIL);
            request.setPassword(PASSWORD);
            request.setRol(rol.name());

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
}