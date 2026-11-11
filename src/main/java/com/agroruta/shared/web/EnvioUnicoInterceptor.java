package com.agroruta.shared.web;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * Evita registros duplicados cuando el teléfono reenvía lo que guardó sin conexión.
 * <p>
 * Los POST de jornales, fumigaciones, cosechas y actividades pueden traer la cabecera
 * {@value #CABECERA} con una clave que el dispositivo genera una sola vez por registro.
 * Si la misma persona manda dos veces la misma clave, la segunda responde 409 (AGR-036)
 * y no llega al controlador. Si el registro falla, la clave se libera para reintentar.
 * Sin cabecera todo funciona como siempre.
 */
@Component
public class EnvioUnicoInterceptor implements HandlerInterceptor {

    public static final String CABECERA = "X-Idempotency-Key";

    private static final String ATRIBUTO = EnvioUnicoInterceptor.class.getName() + ".clave";
    private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private final EnviosRecibidos envios;

    public EnvioUnicoInterceptor(EnviosRecibidos envios) {
        this.envios = envios;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String clave = request.getHeader(CABECERA);
        if (clave == null || !FORMATO.matcher(clave).matches()) return true;
        Long usuarioId = CurrentUser.id(SecurityContextHolder.getContext().getAuthentication());
        if (usuarioId == null) return true;

        if (!envios.anotar(usuarioId, clave)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SUBMISSION, "Este registro ya se había recibido.");
        }
        request.setAttribute(ATRIBUTO, new Object[]{usuarioId, clave});
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(request.getAttribute(ATRIBUTO) instanceof Object[] anotada)) return;
        if (ex != null || response.getStatus() >= 400) {
            envios.olvidar((Long) anotada[0], (String) anotada[1]);
        }
    }
}
