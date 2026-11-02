package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.application.ports.in.PagoUseCase;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.infrastructure.web.dto.PagoRequest;
import com.agroruta.worker.infrastructure.web.dto.PagoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("@ownership.nominaPropia(#req.nominaId, authentication)")
    public ResponseEntity<PagoResponse> registrar(@RequestBody PagoRequest req) {
        PagoResponse response = PagoResponse.from(
                pagoUseCase.registrarPago(
                        req.nominaId, req.monto, req.metodoPago, req.comprobante, req.observaciones
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.pagoPropio(#id, authentication)")
    public ResponseEntity<PagoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(PagoResponse.from(pagoUseCase.buscarPorId(id)));
    }

    @GetMapping("/trabajador/{trabajadorId}")
    @PreAuthorize("@ownership.trabajadorPropio(#trabajadorId, authentication)")
    public ResponseEntity<List<PagoResponse>> historialPorTrabajador(@PathVariable Long trabajadorId) {
        List<PagoResponse> pagos = pagoUseCase.historialPorTrabajador(trabajadorId)
                .stream().map(PagoResponse::from).toList();
        return ResponseEntity.ok(pagos);
    }

    @GetMapping
    public ResponseEntity<List<PagoResponse>> listarTodos(Authentication authentication) {
        List<Pago> lista = CurrentUser.esAdmin(authentication)
                ? pagoUseCase.listarTodos()
                : pagoUseCase.listarTodosDe(CurrentUser.id(authentication));
        return ResponseEntity.ok(lista.stream().map(PagoResponse::from).toList());
    }
}