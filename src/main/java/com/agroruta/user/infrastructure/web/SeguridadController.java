package com.agroruta.user.infrastructure.web;

import com.agroruta.shared.security.CurrentUser;
import com.agroruta.user.application.ports.in.SegundoFactorUseCase;
import com.agroruta.user.application.ports.in.SegundoFactorUseCase.Estado;
import com.agroruta.user.application.ports.in.SegundoFactorUseCase.Preparacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Verificación en dos pasos de la cuenta que tiene la sesión iniciada (cualquier rol). */
@RestController
@RequestMapping("/api/configuration/security/2fa")
public class SeguridadController {

    public record PasswordRequest(@NotBlank(message = "La contraseña es obligatoria") @Size(max = 72) String password) {}

    public record CodigoRequest(@NotBlank(message = "El código es obligatorio") @Size(max = 20) String codigo) {}

    public record DesactivarRequest(@NotBlank(message = "La contraseña es obligatoria") @Size(max = 72) String password,
                                    @NotBlank(message = "El código es obligatorio") @Size(max = 20) String codigo) {}

    private final SegundoFactorUseCase segundoFactor;

    public SeguridadController(SegundoFactorUseCase segundoFactor) {
        this.segundoFactor = segundoFactor;
    }

    @GetMapping
    public ResponseEntity<Estado> estado(Authentication auth) {
        return ResponseEntity.ok(segundoFactor.estado(CurrentUser.id(auth)));
    }

    /** Devuelve el secreto y su QR: no se guarda en caché de ningún lado */
    @PostMapping("/setup")
    public ResponseEntity<Preparacion> preparar(@Valid @RequestBody PasswordRequest req, Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(segundoFactor.preparar(CurrentUser.id(auth), req.password()));
    }

    @PostMapping("/enable")
    public ResponseEntity<Map<String, List<String>>> activar(@Valid @RequestBody CodigoRequest req, Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("codigosRespaldo", segundoFactor.activar(CurrentUser.id(auth), req.codigo())));
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> desactivar(@Valid @RequestBody DesactivarRequest req, Authentication auth) {
        segundoFactor.desactivar(CurrentUser.id(auth), req.password(), req.codigo());
        return ResponseEntity.noContent().build();
    }
}
