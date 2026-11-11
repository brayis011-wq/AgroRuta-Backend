package com.agroruta.user.application;

import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.exception.SesionInvalidaException;
import com.agroruta.user.application.SesionServiceTest.RelojMovil;
import com.agroruta.user.application.SesionServiceTest.SesionesEnMemoria;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.SesionAbierta;
import com.agroruta.user.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Sesiones abiertas")
class SesionesAbiertasTest {

    private final RelojMovil reloj = new RelojMovil();
    private final SesionesEnMemoria repo = new SesionesEnMemoria();
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final String[] dispositivo = {"Chrome · Windows"};
    private SesionService service;

    @BeforeEach
    void preparar() {
        when(usuarios.findById(7L)).thenReturn(Optional.of(new Usuario(7L, "Ana", "ana@x.com", "hash", Rol.Agricultor)));
        service = new SesionService(repo, usuarios, Duration.ofDays(30), reloj, () -> dispositivo[0]);
    }

    @Test
    @DisplayName("lista cada inicio de sesión una vez, con su dispositivo, y la actual primero")
    void lista() {
        String pc = service.abrir(7L);
        reloj.avanzar(Duration.ofHours(1));
        dispositivo[0] = "Safari · iPhone";
        String celular = service.abrir(7L);

        List<SesionAbierta> desdeElPc = service.listar(pc);

        assertEquals(2, desdeElPc.size());
        assertTrue(desdeElPc.get(0).actual());
        assertEquals("Chrome · Windows", desdeElPc.get(0).dispositivo());
        assertEquals("Safari · iPhone", desdeElPc.get(1).dispositivo());
        assertFalse(desdeElPc.get(1).actual());
        assertTrue(service.listar(celular).get(0).dispositivo().startsWith("Safari"));
    }

    @Test
    @DisplayName("al renovar sigue siendo la misma sesión: mismo dispositivo y fecha de inicio, último uso nuevo")
    void renovarConservaLaFamilia() {
        String token = service.abrir(7L);
        SesionAbierta antes = service.listar(token).get(0);
        reloj.avanzar(Duration.ofDays(2));
        dispositivo[0] = "otro navegador";
        String nuevo = service.renovar(token).token();

        List<SesionAbierta> lista = service.listar(nuevo);

        assertEquals(1, lista.size());
        assertEquals("Chrome · Windows", lista.get(0).dispositivo());
        assertEquals(antes.iniciadaEn(), lista.get(0).iniciadaEn());
        assertTrue(lista.get(0).ultimoUso().isAfter(antes.ultimoUso()));
    }

    @Test
    @DisplayName("cerrar otra sesión la deja sin poder renovar y no toca la actual")
    void cerrarOtra() {
        String pc = service.abrir(7L);
        String celular = service.abrir(7L);
        Long idCelular = service.listar(pc).stream().filter(s -> !s.actual()).findFirst().orElseThrow().id();

        assertFalse(service.cerrar(pc, idCelular));

        assertThrows(SesionInvalidaException.class, () -> service.renovar(celular));
        assertEquals(1, service.listar(pc).size());
        assertDoesNotThrow(() -> service.renovar(pc));
    }

    @Test
    @DisplayName("cerrar la sesión actual avisa que era la propia")
    void cerrarLaActual() {
        String pc = service.abrir(7L);
        Long id = service.listar(pc).get(0).id();
        assertTrue(service.cerrar(pc, id));
        assertThrows(SesionInvalidaException.class, () -> service.listar(pc));
    }

    @Test
    @DisplayName("cerrar las demás deja solo la actual")
    void cerrarLasDemas() {
        String pc = service.abrir(7L);
        String celular = service.abrir(7L);
        String tablet = service.abrir(7L);

        assertEquals(2, service.cerrarOtras(pc));

        assertEquals(1, service.listar(pc).size());
        assertThrows(SesionInvalidaException.class, () -> service.renovar(celular));
        assertThrows(SesionInvalidaException.class, () -> service.renovar(tablet));
    }

    @Test
    @DisplayName("nadie cierra la sesión de otra persona")
    void noCierraLaDeOtro() {
        when(usuarios.findById(8L)).thenReturn(Optional.of(new Usuario(8L, "Luis", "luis@x.com", "hash", Rol.Agricultor)));
        String ana = service.abrir(7L);
        String luis = service.abrir(8L);
        Long idDeLuis = service.listar(luis).get(0).id();

        assertThrows(ResourceNotFoundException.class, () -> service.cerrar(ana, idDeLuis));
        assertEquals(1, service.listar(luis).size());
    }

    @Test
    @DisplayName("sin cookie o con una cookie inventada no se ve nada")
    void sinSesion() {
        assertThrows(SesionInvalidaException.class, () -> service.listar(null));
        assertThrows(SesionInvalidaException.class, () -> service.listar("inventado"));
        assertThrows(SesionInvalidaException.class, () -> service.cerrarOtras(""));
    }
}
