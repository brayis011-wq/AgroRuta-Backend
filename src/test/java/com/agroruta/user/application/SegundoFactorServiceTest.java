package com.agroruta.user.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.user.application.SesionServiceTest.RelojMovil;
import com.agroruta.user.application.ports.in.SegundoFactorUseCase.Preparacion;
import com.agroruta.user.application.ports.out.SegundoFactorRepository;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.SegundoFactor;
import com.agroruta.user.domain.Totp;
import com.agroruta.user.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SegundoFactorService")
class SegundoFactorServiceTest {

    static class EnMemoria implements SegundoFactorRepository {
        final Map<Long, SegundoFactor> filas = new HashMap<>();
        @Override public Optional<SegundoFactor> buscar(Long id) { return Optional.ofNullable(filas.get(id)); }
        @Override public SegundoFactor guardar(SegundoFactor s) { filas.put(s.getUsuarioId(), s); return s; }
        @Override public void borrar(Long id) { filas.remove(id); }
    }

    private final RelojMovil reloj = new RelojMovil();
    private final EnMemoria repo = new EnMemoria();
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private SegundoFactorService service;

    @BeforeEach
    void preparar() {
        when(usuarios.findById(7L)).thenReturn(Optional.of(new Usuario(7L, "Ana", "ana@x.com", "hash", Rol.Administrador)));
        when(encoder.matches("clave-buena", "hash")).thenReturn(true);
        service = new SegundoFactorService(repo, usuarios, encoder, "secreto-del-servidor-para-pruebas", reloj);
    }

    private String codigoDeLaApp(String secreto) {
        return Totp.codigo(secreto, Totp.paso(reloj.instant().getEpochSecond()));
    }

    /** Deja el segundo factor activo y devuelve [secreto, códigos de respaldo...] */
    private String activar(List<String> respaldo) {
        Preparacion p = service.preparar(7L, "clave-buena");
        respaldo.addAll(service.activar(7L, codigoDeLaApp(p.secreto())));
        return p.secreto();
    }

    @Test
    @DisplayName("preparar pide la contraseña y entrega secreto, enlace y QR; todavía no queda activo")
    void prepara() {
        assertThrows(BusinessException.class, () -> service.preparar(7L, "otra"));

        Preparacion p = service.preparar(7L, "clave-buena");

        assertEquals(32, p.secreto().length());
        assertTrue(p.uri().startsWith("otpauth://totp/AgroRuta%3Aana%40x.com?secret=" + p.secreto()));
        assertTrue(p.qr().startsWith("data:image/svg+xml;base64,"));
        assertFalse(service.activo(7L));
        assertNotEquals(p.secreto(), repo.filas.get(7L).getSecretoCifrado());
    }

    @Test
    @DisplayName("se activa con un código de la app y entrega 8 códigos de respaldo")
    void activa() {
        Preparacion p = service.preparar(7L, "clave-buena");
        BusinessException e = assertThrows(BusinessException.class, () -> service.activar(7L, "000000"));
        assertEquals(ErrorCode.TWO_FACTOR_INVALID, e.getErrorCode());

        List<String> codigos = service.activar(7L, codigoDeLaApp(p.secreto()));

        assertEquals(8, codigos.size());
        assertTrue(codigos.get(0).matches("[A-Z2-9]{5}-[A-Z2-9]{5}"));
        assertTrue(service.activo(7L));
        assertEquals(8, service.estado(7L).codigosRespaldo());
        // En la base quedan los hashes, no los códigos
        assertFalse(repo.filas.get(7L).getRespaldo().contains(codigos.get(0).replace("-", "")));
    }

    @Test
    @DisplayName("al entrar acepta el código de la app una sola vez")
    void verificaUnaVez() {
        String secreto = activar(new java.util.ArrayList<>());
        reloj.avanzar(Duration.ofSeconds(60));
        String codigo = codigoDeLaApp(secreto);

        assertTrue(service.verificar(7L, " " + codigo.substring(0, 3) + " " + codigo.substring(3)));
        assertFalse(service.verificar(7L, codigo));
        assertFalse(service.verificar(7L, "123456"));
        assertFalse(service.verificar(7L, ""));
    }

    @Test
    @DisplayName("un código de respaldo sirve una vez, con o sin guion, y se descuenta")
    void respaldo() {
        List<String> respaldo = new java.util.ArrayList<>();
        activar(respaldo);

        assertTrue(service.verificar(7L, respaldo.get(0).toLowerCase()));
        assertFalse(service.verificar(7L, respaldo.get(0)));
        assertTrue(service.verificar(7L, respaldo.get(1).replace("-", "")));
        assertEquals(6, service.estado(7L).codigosRespaldo());
    }

    @Test
    @DisplayName("una cuenta sin segundo factor pasa sin código")
    void sinSegundoFactor() {
        assertTrue(service.verificar(7L, null));
        assertFalse(service.estado(7L).activo());
    }

    @Test
    @DisplayName("para quitarlo hacen falta la contraseña y un código")
    void desactiva() {
        String secreto = activar(new java.util.ArrayList<>());
        reloj.avanzar(Duration.ofSeconds(60));

        assertThrows(BusinessException.class, () -> service.desactivar(7L, "otra", codigoDeLaApp(secreto)));
        assertThrows(BusinessException.class, () -> service.desactivar(7L, "clave-buena", "000000"));
        assertTrue(service.activo(7L));

        service.desactivar(7L, "clave-buena", codigoDeLaApp(secreto));
        assertFalse(service.activo(7L));
    }

    @Test
    @DisplayName("no se puede volver a preparar si ya está activo")
    void yaActivo() {
        activar(new java.util.ArrayList<>());
        assertThrows(BusinessException.class, () -> service.preparar(7L, "clave-buena"));
    }

    @Test
    @DisplayName("el secreto cifrado se puede leer de vuelta y cada cifrado es distinto")
    void cifrado() {
        String a = service.cifrar("GEZDGNBVGY3TQOJQ");
        String b = service.cifrar("GEZDGNBVGY3TQOJQ");
        assertNotEquals(a, b);
        assertEquals("GEZDGNBVGY3TQOJQ", service.descifrar(a));
        assertThrows(IllegalStateException.class, () -> service.descifrar("AAAA" + a.substring(4)));
    }
}
