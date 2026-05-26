package com.agroruta.user.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserService userService;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long   ID       = 1L;
    private static final String NOMBRE   = "Juan Pérez";
    private static final String EMAIL    = "juan@agroruta.com";
    private static final String PASSWORD = "$2a$10$hashedpassword";
    private static final Rol    ROL      = Rol.Agricultor;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario(ID, NOMBRE, EMAIL, PASSWORD, ROL);
    }

    // ── registrar ─────────────────────────────────────────────────

    @Test
    void deberiaRegistrarUsuarioCuandoElEmailNoExiste() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        Usuario resultado = userService.registrar(usuario);

        assertEquals(ID,     resultado.getId());
        assertEquals(EMAIL,  resultado.getEmail());
        assertEquals(NOMBRE, resultado.getNombre());
        assertEquals(ROL,    resultado.getRol());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void deberiaLanzarBusinessExceptionSiElEmailYaExiste() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.registrar(usuario)
        );

        assertTrue(ex.getMessage().contains("correo electrónico ya está registrado"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarDeberiaVerificarEmailAntesDeGuardar() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        userService.registrar(usuario);

        InOrder orden = inOrder(usuarioRepository);
        orden.verify(usuarioRepository).findByEmail(EMAIL);
        orden.verify(usuarioRepository).save(usuario);
    }

    @Test
    void registrarDeberiaGuardarElUsuarioActivoPorDefecto() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        userService.registrar(usuario);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertTrue(captor.getValue().isActivo());
    }

    // ── buscarPorId ───────────────────────────────────────────────

    @Test
    void deberiaRetornarUsuarioCuandoExisteElId() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));

        Usuario resultado = userService.buscarPorId(ID);

        assertEquals(ID,     resultado.getId());
        assertEquals(EMAIL,  resultado.getEmail());
        assertEquals(NOMBRE, resultado.getNombre());
    }

    @Test
    void deberiaLanzarResourceNotFoundExceptionSiElIdNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.buscarPorId(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── buscarPorEmail ────────────────────────────────────────────

    @Test
    void deberiaRetornarUsuarioCuandoExisteElEmail() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        Usuario resultado = userService.buscarPorEmail(EMAIL);

        assertEquals(ID,    resultado.getId());
        assertEquals(EMAIL, resultado.getEmail());
    }

    @Test
    void deberiaLanzarResourceNotFoundExceptionSiElEmailNoExiste() {
        String emailInexistente = "noexiste@agroruta.com";
        when(usuarioRepository.findByEmail(emailInexistente)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.buscarPorEmail(emailInexistente)
        );

        assertTrue(ex.getMessage().contains(emailInexistente));
    }

    // ── listarActivos ─────────────────────────────────────────────

    @Test
    void deberiaRetornarListaDeUsuariosActivos() {
        Usuario usuario2 = new Usuario(2L, "Ana López", "ana@agroruta.com", PASSWORD, Rol.Trabajador);
        when(usuarioRepository.findAllActivos()).thenReturn(List.of(usuario, usuario2));

        List<Usuario> resultado = userService.listarActivos();

        assertEquals(2,      resultado.size());
        assertEquals(ID,     resultado.get(0).getId());
        assertEquals("Ana López", resultado.get(1).getNombre());
        verify(usuarioRepository, times(1)).findAllActivos();
    }

    @Test
    void deberiaRetornarListaVaciaSiNoHayUsuariosActivos() {
        when(usuarioRepository.findAllActivos()).thenReturn(List.of());

        List<Usuario> resultado = userService.listarActivos();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ── desactivarUsuario ─────────────────────────────────────────

    @Test
    void deberiaDesactivarUsuarioExistente() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        userService.desactivarUsuario(ID);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertFalse(captor.getValue().isActivo());
    }

    @Test
    void desactivarDeberiaLanzarExcepcionSiElUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.desactivarUsuario(99L)
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desactivarDeberiaGuardarDespuesDeDesactivar() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        userService.desactivarUsuario(ID);

        InOrder orden = inOrder(usuarioRepository);
        orden.verify(usuarioRepository).findById(ID);
        orden.verify(usuarioRepository).save(any(Usuario.class));
    }

    // ── activarUsuario ────────────────────────────────────────────

    @Test
    void deberiaActivarUsuarioDesactivado() {
        usuario.desactivar();
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        userService.activarUsuario(ID);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertTrue(captor.getValue().isActivo());
    }

    @Test
    void activarDeberiaLanzarExcepcionSiElUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.activarUsuario(99L)
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void activarDeberiaGuardarDespuesDeActivar() {
        usuario.desactivar();
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        userService.activarUsuario(ID);

        InOrder orden = inOrder(usuarioRepository);
        orden.verify(usuarioRepository).findById(ID);
        orden.verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deberiaActivarUsuarioQueYaEstabaActivo() {
        assertTrue(usuario.isActivo());
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        userService.activarUsuario(ID);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertTrue(captor.getValue().isActivo());
    }
}