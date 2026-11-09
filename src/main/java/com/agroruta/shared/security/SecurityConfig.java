package com.agroruta.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Tareas programadas (GitHub Actions): las protege la clave TAREAS_TOKEN, no una sesión
                        .requestMatchers("/api/jobs/**").permitAll()
                        // El clima usa nuestra clave de OpenWeather: solo con sesión iniciada, para que nadie gaste la cuota desde afuera
                        .requestMatchers("/api/weather/**").authenticated()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Solo el health es público (healthchecks de Docker/Render)
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("Administrador")
                        .requestMatchers("/api/backups/**").hasRole("Administrador")
                        .requestMatchers("/api/admin/**").hasRole("Administrador")
                        // Portal del trabajador: solo el rol Trabajador
                        .requestMatchers("/api/portal/**").hasRole("Trabajador")
                        // Pantallas de gestión: agricultores y administrador (el trabajador no gestiona nada)
                        .requestMatchers(
                                "/api/farms/**", "/api/plots/**", "/api/plantings/**", "/api/harvests/**",
                                "/api/sprayings/**", "/api/activities/**", "/api/agricultural-inputs/**",
                                "/api/workers/**", "/api/positions/**", "/api/workdays/**", "/api/payrolls/**",
                                "/api/payments/**", "/api/reports/**", "/api/crop-catalog/**",
                                "/api/inventory/**", "/api/labors/**"
                        ).hasAnyRole("Agricultor", "Administrador")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}