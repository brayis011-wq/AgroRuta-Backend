package com.agroruta.configuration.domain;

import com.agroruta.user.domain.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias - Perfil")
class PerfilTest {

    private Perfil perfil;

    @BeforeEach
    void setUp() {
        perfil = new Perfil(
                1L,
                "Carlos Pérez",
                "carlos@agroruta.com",
                "3001234567",
                "foto.jpg",
                Rol.Agricultor
        );
    }

    @Test
    @DisplayName("Constructor vacío debe crear instancia con todos los campos en null")
    void constructorVacio_debeCrearInstanciaLimpia() {
        Perfil vacio = new Perfil();

        assertNotNull(vacio);
        assertNull(vacio.getId());
        assertNull(vacio.getNombre());
        assertNull(vacio.getEmail());
        assertNull(vacio.getTelefono());
        assertNull(vacio.getFotoPerfil());
        assertNull(vacio.getRol());
    }

    @Test
    @DisplayName("Constructor completo debe asignar correctamente todos los campos")
    void constructorCompleto_debeAsignarTodosLosCampos() {
        assertEquals(1L,                    perfil.getId());
        assertEquals("Carlos Pérez",        perfil.getNombre());
        assertEquals("carlos@agroruta.com", perfil.getEmail());
        assertEquals("3001234567",          perfil.getTelefono());
        assertEquals("foto.jpg",            perfil.getFotoPerfil());
        assertEquals(Rol.Agricultor,        perfil.getRol());
    }

    @Test
    @DisplayName("actualizarDatos debe modificar nombre, teléfono y foto sin alterar email ni rol")
    void actualizarDatos_debeSoloModificarLosTresCampos() {
        perfil.actualizarDatos("Luis Gómez", "3109876543", "nueva_foto.png");

        assertEquals("Luis Gómez",          perfil.getNombre());
        assertEquals("3109876543",          perfil.getTelefono());
        assertEquals("nueva_foto.png",      perfil.getFotoPerfil());

        assertEquals("carlos@agroruta.com", perfil.getEmail());
        assertEquals(Rol.Agricultor,        perfil.getRol());
        assertEquals(1L,                    perfil.getId());
    }

    @Test
    @DisplayName("actualizarDatos debe aceptar valores null en campos opcionales")
    void actualizarDatos_conNullEnCamposOpcionales() {
        perfil.actualizarDatos("Ana Torres", null, null);

        assertEquals("Ana Torres", perfil.getNombre());
        assertNull(perfil.getTelefono());
        assertNull(perfil.getFotoPerfil());
    }

    @Test
    @DisplayName("Setters deben actualizar cada campo de forma independiente")
    void setters_debenActualizarCadaCampoIndependientemente() {
        Perfil p = new Perfil();

        p.setId(99L);
        p.setNombre("María Ruiz");
        p.setEmail("maria@agroruta.com");
        p.setTelefono("3205551234");
        p.setFotoPerfil("avatar.png");
        p.setRol(Rol.Trabajador);

        assertEquals(99L,                   p.getId());
        assertEquals("María Ruiz",          p.getNombre());
        assertEquals("maria@agroruta.com",  p.getEmail());
        assertEquals("3205551234",          p.getTelefono());
        assertEquals("avatar.png",          p.getFotoPerfil());
        assertEquals(Rol.Trabajador,        p.getRol());
    }

    @Test
    @DisplayName("Debe permitir crear un Perfil con rol null")
    void perfil_conRolNull_debeSerValido() {
        Perfil sinRol = new Perfil(2L, "Pedro", "pedro@test.com",
                null, null, null);

        assertNotNull(sinRol);
        assertNull(sinRol.getRol());
        assertNull(sinRol.getTelefono());
    }

    @Test
    @DisplayName("actualizarDatos debe aceptar cadenas vacías como valores válidos")
    void actualizarDatos_conCadenasVacias() {
        perfil.actualizarDatos("", "", "");

        assertEquals("", perfil.getNombre());
        assertEquals("", perfil.getTelefono());
        assertEquals("", perfil.getFotoPerfil());
    }

    @Test
    @DisplayName("Debe verificar los tres valores del enum Rol")
    void rol_debeContenerTodosLosValoresEsperados() {
        Rol[] valores = Rol.values();

        assertEquals(3, valores.length);
        assertEquals(Rol.Administrador, Rol.valueOf("Administrador"));
        assertEquals(Rol.Agricultor,    Rol.valueOf("Agricultor"));
        assertEquals(Rol.Trabajador,    Rol.valueOf("Trabajador"));
    }
}