package com.agroruta.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void deberiaCrearUsuarioConValoresCorrectos() {
        Usuario usuario = new Usuario(1L, "Juan", "juan@mail.com", "pass123", Rol.Agricultor);

        assertEquals(1L, usuario.getId());
        assertEquals("Juan", usuario.getNombre());
        assertEquals("juan@mail.com", usuario.getEmail());
        assertEquals("pass123", usuario.getPassword());
        assertEquals(Rol.Agricultor, usuario.getRol());
    }

    @Test
    void deberiaEstarActivoAlCrearse() {
        Usuario usuario = new Usuario(1L, "Juan", "juan@mail.com", "pass123", Rol.Agricultor);

        assertTrue(usuario.isActivo());
    }

    @Test
    void deberiaTenerFechaCreacionAlCrearse() {
        Usuario usuario = new Usuario(1L, "Juan", "juan@mail.com", "pass123", Rol.Agricultor);

        assertNotNull(usuario.getFechaCreacion());
    }

    @Test
    void deberiaDesactivarUsuario() {
        Usuario usuario = new Usuario(1L, "Juan", "juan@mail.com", "pass123", Rol.Agricultor);

        usuario.desactivar();

        assertFalse(usuario.isActivo());
    }

    @Test
    void deberiaActivarUsuarioDesactivado() {
        Usuario usuario = new Usuario(1L, "Juan", "juan@mail.com", "pass123", Rol.Agricultor);
        usuario.desactivar();

        usuario.activar();

        assertTrue(usuario.isActivo());
    }

    @Test
    void deberiaPermitirCambiarNombre() {
        Usuario usuario = new Usuario(1L, "Juan", "juan@mail.com", "pass123", Rol.Agricultor);

        usuario.setNombre("Pedro");

        assertEquals("Pedro", usuario.getNombre());
    }

    @Test
    void deberiaCrearUsuarioVacioConNoArgsConstructor() {
        Usuario usuario = new Usuario();

        assertNull(usuario.getId());
        assertNull(usuario.getNombre());
        assertFalse(usuario.isActivo());
    }
}