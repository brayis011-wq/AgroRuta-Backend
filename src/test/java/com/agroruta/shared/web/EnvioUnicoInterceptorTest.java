package com.agroruta.shared.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnvioUnicoInterceptorTest {

    /** Claves anotadas, en memoria */
    private static class EnMemoria implements EnviosRecibidos {
        final Set<String> claves = new HashSet<>();

        @Override
        public boolean anotar(Long usuarioId, String clave) {
            return claves.add(usuarioId + "|" + clave);
        }

        @Override
        public void olvidar(Long usuarioId, String clave) {
            claves.remove(usuarioId + "|" + clave);
        }
    }

    private static final String CLAVE = "0b6f3f0e-2a55-4c0c-9a51-1f6f6f0c2d11";

    private final EnMemoria envios = new EnMemoria();
    private final EnvioUnicoInterceptor interceptor = new EnvioUnicoInterceptor(envios);

    @BeforeEach
    void sesionDe() {
        entrarComo(7L);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void entrarComo(Long id) {
        CustomUserDetails usuario = mock(CustomUserDetails.class);
        when(usuario.getId()).thenReturn(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private static MockHttpServletRequest post(String clave) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/harvests");
        if (clave != null) req.addHeader(EnvioUnicoInterceptor.CABECERA, clave);
        return req;
    }

    @Test
    void sinCabecera_pasaComoSiempre() {
        assertThat(interceptor.preHandle(post(null), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(envios.claves).isEmpty();
    }

    @Test
    void primeraVez_pasaYQuedaAnotada() {
        MockHttpServletRequest req = post(CLAVE);
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(201);

        assertThat(interceptor.preHandle(req, res, new Object())).isTrue();
        interceptor.afterCompletion(req, res, new Object(), null);

        assertThat(envios.claves).containsExactly("7|" + CLAVE);
    }

    @Test
    void mismaClaveDosVeces_laSegundaSeRechaza() {
        MockHttpServletRequest primera = post(CLAVE);
        MockHttpServletResponse ok = new MockHttpServletResponse();
        interceptor.preHandle(primera, ok, new Object());
        interceptor.afterCompletion(primera, ok, new Object(), null);

        assertThatThrownBy(() -> interceptor.preHandle(post(CLAVE), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_SUBMISSION));
    }

    @Test
    void siElRegistroFalla_laClaveQuedaLibreParaReintentar() {
        MockHttpServletRequest req = post(CLAVE);
        MockHttpServletResponse rechazo = new MockHttpServletResponse();
        interceptor.preHandle(req, rechazo, new Object());
        rechazo.setStatus(422);
        interceptor.afterCompletion(req, rechazo, new Object(), null);

        assertThat(envios.claves).isEmpty();
        assertThat(interceptor.preHandle(post(CLAVE), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void siElControladorLanzaUnaExcepcion_tambienSeLibera() {
        MockHttpServletRequest req = post(CLAVE);
        MockHttpServletResponse res = new MockHttpServletResponse();
        interceptor.preHandle(req, res, new Object());
        interceptor.afterCompletion(req, res, new Object(), new IllegalStateException("boom"));

        assertThat(envios.claves).isEmpty();
    }

    @Test
    void laMismaClaveDeOtraPersonaNoChoca() {
        interceptor.preHandle(post(CLAVE), new MockHttpServletResponse(), new Object());
        entrarComo(8L);

        assertThat(interceptor.preHandle(post(CLAVE), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(envios.claves).containsExactlyInAnyOrder("7|" + CLAVE, "8|" + CLAVE);
    }

    @Test
    void claveConFormatoRaro_seIgnora() {
        assertThat(interceptor.preHandle(post("x y; drop"), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(envios.claves).isEmpty();
    }

    @Test
    void otrosMetodosNoSeTocan() {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/harvests");
        get.addHeader(EnvioUnicoInterceptor.CABECERA, CLAVE);

        assertThat(interceptor.preHandle(get, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(envios.claves).isEmpty();
    }
}
