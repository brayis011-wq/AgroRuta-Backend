package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.PresupuestoService;
import com.agroruta.crop.domain.PresupuestoSiembra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Presupuesto de una temporada frente a lo que lleva gastado. */
@RestController
@RequestMapping("/api/plantings/{siembraId}/budget")
public class PresupuestoController {

    private final PresupuestoService presupuestos;

    public PresupuestoController(PresupuestoService presupuestos) {
        this.presupuestos = presupuestos;
    }

    public record PresupuestoRequest(
            @PositiveOrZero(message = "No puede ser negativo")
            @DecimalMax("100000000000")
            @Digits(integer = 12, fraction = 2)
            BigDecimal manoObra,

            @PositiveOrZero(message = "No puede ser negativo")
            @DecimalMax("100000000000")
            @Digits(integer = 12, fraction = 2)
            BigDecimal insumos,

            @PositiveOrZero(message = "No puede ser negativo")
            @DecimalMax("100000000000")
            @Digits(integer = 12, fraction = 2)
            BigDecimal otros) {}

    public record PresupuestoResponse(
            Long siembraId,
            BigDecimal manoObra,
            BigDecimal insumos,
            BigDecimal otros,
            BigDecimal total,
            double gastadoManoObra,
            double gastadoInsumos,
            double gastadoOtros,
            double gastadoTotal,
            Double porcentaje,
            PresupuestoSiembra.Estado estado,
            boolean manoObraSuperada,
            boolean insumosSuperados,
            boolean otrosSuperados) {
        static PresupuestoResponse from(PresupuestoSiembra.Comparacion c) {
            PresupuestoSiembra p = c.presupuesto();
            return new PresupuestoResponse(
                    p.siembraId(),
                    p.manoObra(),
                    p.insumos(),
                    p.otros(),
                    p.total(),
                    c.gastadoManoObra(),
                    c.gastadoInsumos(),
                    c.gastadoOtros(),
                    c.gastadoTotal(),
                    c.porcentaje(),
                    c.estado(),
                    c.manoObraSuperada(),
                    c.insumosSuperados(),
                    c.otrosSuperados());
        }
    }

    /** 204 si la temporada no tiene presupuesto */
    @GetMapping
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<PresupuestoResponse> obtener(@PathVariable Long siembraId) {
        return presupuestos
                .comparacion(siembraId)
                .map(PresupuestoResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<PresupuestoResponse> guardar(
            @PathVariable Long siembraId, @Valid @RequestBody PresupuestoRequest req) {
        return ResponseEntity.ok(
                PresupuestoResponse.from(presupuestos.guardar(siembraId, req.manoObra(), req.insumos(), req.otros())));
    }

    @DeleteMapping
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<Void> borrar(@PathVariable Long siembraId) {
        presupuestos.borrar(siembraId);
        return ResponseEntity.noContent().build();
    }
}
