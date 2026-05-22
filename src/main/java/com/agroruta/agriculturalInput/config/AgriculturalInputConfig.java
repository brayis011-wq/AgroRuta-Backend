package com.agroruta.agriculturalInput.config;

import com.agroruta.agriculturalInput.application.ports.out.AgriculturalInputRepositoryPort;
import com.agroruta.agriculturalInput.application.AgriculturalInputService;
import com.agroruta.agriculturalInput.infrastructure.persistence.AgriculturalInputJpaRepository;
import com.agroruta.agriculturalInput.infrastructure.persistence.AgriculturalInputRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración de Spring para el módulo agricultural-input.
 *
 * ¿Por qué aquí y no en @Service?
 * Para mantener el dominio y la aplicación libres de anotaciones de framework.
 * Este archivo es el único punto donde Spring "conoce" el módulo.
 * Si en el futuro se migra a Quarkus u otro framework, solo se toca esta clase.
 */
@Configuration
public class AgriculturalInputConfig {

    /**
     * Adaptador de persistencia: implementa el puerto de salida con JPA.
     */
    @Bean
    public AgriculturalInputRepositoryPort agriculturalInputRepositoryPort(
            AgriculturalInputJpaRepository jpaRepository) {
        return new AgriculturalInputRepositoryAdapter(jpaRepository);
    }

    /**
     * Servicio de aplicación: orquesta todos los casos de uso.
     * Registrado una sola vez; implementa 6 interfaces (los casos de uso).
     * Spring lo inyecta por la interfaz que pide cada constructor.
     */
    @Bean
    public AgriculturalInputService agriculturalInputService(
            AgriculturalInputRepositoryPort repositoryPort) {
        return new AgriculturalInputService(repositoryPort);
    }
}