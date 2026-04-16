package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.application.ports.in.PagoUseCase;
import com.agroruta.worker.infrastructure.web.dto.PagoRequest;
import com.agroruta.worker.infrastructure.web.dto.PagoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoUseCase pagoUseCase;

    public PagoController(PagoUseCase pagoUseCase) {
        this.pagoUseCase = pagoUseCase;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> registrar(@RequestBody PagoRequest req) {
        PagoResponse response = PagoResponse.from(
                pagoUseCase.registrarPago(
                        req.nominaId, req.monto, req.metodoPago, req.comprobante, req.observaciones
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(PagoResponse.from(pagoUseCase.buscarPorId(id)));
    }

    @GetMapping("/trabajador/{trabajadorId}")
    public ResponseEntity<List<PagoResponse>> historialPorTrabajador(@PathVariable Long trabajadorId) {
        List<PagoResponse> pagos = pagoUseCase.historialPorTrabajador(trabajadorId)
                .stream().map(PagoResponse::from).toList();
        return ResponseEntity.ok(pagos);
    }

    @GetMapping
    public ResponseEntity<List<PagoResponse>> listarTodos() {
        List<PagoResponse> pagos = pagoUseCase.listarTodos()
                .stream().map(PagoResponse::from).toList();
        return ResponseEntity.ok(pagos);
    }
}