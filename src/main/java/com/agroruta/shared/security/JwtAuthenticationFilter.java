package com.agroruta.shared.security;

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

// @Component le dice a Spring que maneje esta clase y la inyecte donde sea necesaria
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // Inyectamos nuestro JwtService y el servicio que busca al usuario en la BD
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

        // 1. Buscamos el token en la cabecera (Header) de la petición que se llama "Authorization"
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Verificamos si la cabecera existe y si empieza con la palabra "Bearer " (el estándar de JWT)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Si no hay token, lo dejamos seguir, pero como anónimo. Spring Security lo bloqueará más adelante si la ruta es privada.
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraemos el token (cortamos los primeros 7 caracteres de "Bearer ")
        jwt = authHeader.substring(7);

        // 4. Extraemos el correo del usuario (usando la clase que creamos antes)
        userEmail = jwtService.extractUsername(jwt);

        // 5. Si encontramos un correo y el usuario AÚN NO está autenticado en este contexto
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Buscamos al usuario en la base de datos
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Validamos que el token sea correcto y no haya expirado
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Creamos el "Pase VIP" oficial para Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Le agregamos detalles extras sobre la petición original (como la IP)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // ¡Aprobado! Guardamos la autenticación en el contexto de Spring
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 6. Pasamos la petición al siguiente filtro o controlador
        filterChain.doFilter(request, response);
    }
}