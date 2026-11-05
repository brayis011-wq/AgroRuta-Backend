package com.agroruta.notification.infrastructure.correo;

import com.agroruta.notification.application.ports.out.CorreoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class CorreoConfig {

    private static final Logger log = LoggerFactory.getLogger(CorreoConfig.class);

    /** Pocos hilos: Gmail limita los envíos y Render gratis tiene poca memoria */
    @Bean(name = "correos")
    public Executor correos() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("correo-");
        ex.initialize();
        return ex;
    }

    @Bean
    public CorreoPort correoPort(@Value("${correo.gmail.client-id:}") String clientId,
                                 @Value("${correo.gmail.client-secret:}") String clientSecret,
                                 @Value("${correo.gmail.refresh-token:}") String refreshToken,
                                 @Value("${correo.remitente:}") String remitente,
                                 @Value("${correo.log-contenido:false}") boolean logContenido) {
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            log.warn("Gmail no está configurado (GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET, GMAIL_REFRESH_TOKEN): "
                    + "los correos solo quedan en el log");
            return new LogCorreoAdapter(logContenido);
        }
        log.info("Correos con la API de Gmail{}", remitente.isBlank() ? "" : " desde " + remitente);
        return new GmailApiCorreoAdapter(clientId.trim(), clientSecret.trim(), refreshToken.trim(), remitente.trim());
    }
}
