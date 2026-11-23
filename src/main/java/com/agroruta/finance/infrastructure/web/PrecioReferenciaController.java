package com.agroruta.finance.infrastructure.web;

import com.agroruta.finance.application.ports.in.PreciosReferenciaUseCase;
import com.agroruta.finance.domain.PrecioCultivo;
import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.finance.domain.PreferenciaPrecio;
import com.agroruta.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Precio de referencia de lo cosechado: el promedio mayorista semanal de SIPSA (DANE) o el precio
 * que el agricultor fije para su cultivo.
 */
@RestController
@RequestMapping("/api/reference-prices")
@PreAuthorize("hasAnyRole('Agricultor','Administrador')")
public class PrecioReferenciaController {

    private final PreciosReferenciaUseCase precios;

    public PrecioReferenciaController(PreciosReferenciaUseCase precios) {
        this.precios = precios;
    }

    /** ?crop=Aguacate&variety=Hass */
    @GetMapping("/crop")
    public ResponseEntity<PrecioCultivo> deCultivo(@RequestParam("crop") String cultivo,
                                                   @RequestParam(value = "variety", required = false) String variedad,
                                                   Authentication authentication) {
        return ResponseEntity.ok(precios.precioDe(CurrentUser.id(authentication), cultivo, variedad));
    }

    /** Buscar un producto de SIPSA para elegirlo a mano: ?q=papa */
    @GetMapping
    public ResponseEntity<List<PrecioReferencia>> buscar(@RequestParam("q") String texto) {
        return ResponseEntity.ok(precios.buscar(texto));
    }

    /** Lo que el agricultor eligió para cada cultivo */
    @GetMapping("/mine")
    public ResponseEntity<List<PreferenciaPrecio>> mias(Authentication authentication) {
        return ResponseEntity.ok(precios.preferencias(CurrentUser.id(authentication)));
    }

    public record EleccionRequest(
            @NotBlank(message = "Indica el cultivo") @Size(max = 80) String cultivo,
            @Size(max = 120) String producto,
            @Positive(message = "El precio debe ser mayor a 0")
            @DecimalMax(value = "100000000", message = "El precio es demasiado alto")
            @Digits(integer = 12, fraction = 2, message = "El precio admite máximo 2 decimales") BigDecimal precioKg) {}

    @PutMapping("/mine")
    @PreAuthorize("hasRole('Agricultor')")
    public ResponseEntity<PreferenciaPrecio> elegir(@Valid @RequestBody EleccionRequest req, Authentication authentication) {
        return ResponseEntity.ok(precios.elegir(CurrentUser.id(authentication), req.cultivo(), req.producto(), req.precioKg()));
    }

    /** Vuelve a la búsqueda automática: ?crop=Café */
    @DeleteMapping("/mine")
    @PreAuthorize("hasRole('Agricultor')")
    public ResponseEntity<Void> quitar(@RequestParam("crop") String cultivo, Authentication authentication) {
        precios.quitarPreferencia(CurrentUser.id(authentication), cultivo);
        return ResponseEntity.noContent().build();
    }

    /** El Administrador trae los precios de SIPSA en el momento (tarda unos 20 segundos) */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Map<String, Integer>> actualizar() {
        return ResponseEntity.ok(Map.of("precios", precios.actualizar()));
    }
}
