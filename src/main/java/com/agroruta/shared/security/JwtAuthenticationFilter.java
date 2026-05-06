package com.agroruta.shared.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // @Lazy en UserDetailsService rompe el ciclo de dependencias circular con SecurityConfig
    public JwtAuthenticationFilter(JwtService jwtService, @Lazy UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Buscamos el token en la cabecera Authorization
        final String authHeader = request.getHeader("Authorization");

        // 2. Si no hay token o no empieza con "Bearer ", dejamos pasar como anónimo
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraemos el token (cortamos los primeros 7 caracteres de "Bearer ")
        final String jwt = authHeader.substring(7);

        try {
            // 4. Extraemos el correo del usuario
            final String userEmail = jwtService.extractUsername(jwt);

            // 5. Si encontramos un correo y el usuario AÚN NO está autenticado en este contexto
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Buscamos al usuario en la base de datos
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Validamos que el token sea correcto y no haya expirado
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Creamos el token de autenticación para Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Agregamos detalles de la petición (IP, session, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Guardamos la autenticación en el contexto de Spring
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // 6. Pasamos la petición al siguiente filtro o controlador
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            // Token expirado — responde 401 directamente sin redirigir a /error
            sendUnauthorizedResponse(response, "Token expirado, inicia sesión nuevamente");

        } catch (JwtException ex) {
            // Token malformado, firma inválida, etc.
            sendUnauthorizedResponse(response, "Token inválido");
        }
    }

    /**
     * Escribe una respuesta 401 en formato JSON directamente en el response.
     * Evita que Spring redirija a /error y cause un 403 secundario.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {
                  "status": 401,
                  "errorCode": "AGR-401",
                  "message": "%s"
                }
                """.formatted(message));
    }
}