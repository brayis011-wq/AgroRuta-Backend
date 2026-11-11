package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.ConsejosClimaUseCase;
import com.agroruta.shared.security.CurrentUser;
import com.agroruta.weather.domain.ConsejoClima;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lo que el clima cambia en la agenda de hoy y mañana del agricultor que pregunta. */
@RestController
public class ConsejosClimaController {

    private final ConsejosClimaUseCase consejos;

    public ConsejosClimaController(ConsejosClimaUseCase consejos) {
        this.consejos = consejos;
    }

    @GetMapping("/api/weather/advice")
    public ResponseEntity<List<ConsejoClima>> consejos(Authentication auth) {
        return ResponseEntity.ok(consejos.consejos(CurrentUser.id(auth)));
    }
}
