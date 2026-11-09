package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.domain.ResumenPagos;
import com.agroruta.shared.util.Pagina;
import jakarta.validation.Valid;
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
@RequestMapping("/api/payments")
public class PagoController {

    private final PagoUseCase pagoUseCase;

    public PagoController(PagoUseCase pagoUseCase) {
        this.pagoUseCase = pagoUseCase;
    }

    @PostMapping
    @PreAuthorize("@ownership.nominaPropia(#req.nominaId, authentication)")
    public ResponseEntity<PagoResponse> registrar(@Valid @RequestBody PagoRequest req) {
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

    @GetMapping("/worker/{trabajadorId}")
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

    /** Lista de pagos por páginas, más recientes primero. Filtro opcional: la nómina. */
    @GetMapping("/page")
    public ResponseEntity<Pagina<PagoResponse>> pagina(@RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size,
                                                       @RequestParam(required = false) Long nominaId,
                                                       Authentication authentication) {
        Long dueno = CurrentUser.esAdmin(authentication) ? null : CurrentUser.id(authentication);
        return ResponseEntity.ok(pagoUseCase.pagina(dueno, nominaId, Pagina.numero(page), Pagina.tamano(size))
                .map(PagoResponse::from));
    }

    /** Totales de los pagos para las tarjetas de la lista, con el mismo filtro de nómina. */
    @GetMapping("/summary")
    public ResponseEntity<ResumenPagos> resumen(@RequestParam(required = false) Long nominaId,
                                                Authentication authentication) {
        Long dueno = CurrentUser.esAdmin(authentication) ? null : CurrentUser.id(authentication);
        return ResponseEntity.ok(pagoUseCase.resumen(dueno, nominaId));
    }
}
