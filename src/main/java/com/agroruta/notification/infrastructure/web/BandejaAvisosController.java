package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.BandejaAvisosUseCase;
import com.agroruta.notification.domain.AvisoBandeja;
import com.agroruta.shared.security.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** La bandeja de avisos (la campana) de quien inició sesión, en cualquier rol. Cada quien ve solo los suyos. */
@RestController
@RequestMapping("/api/notifications/inbox")
public class BandejaAvisosController {

    /**
     * Un aviso tal como lo muestra la campana. Las fechas van como texto ISO (UTC) para no depender
     * de cómo esté configurado Jackson.
     */
    public record AvisoResponse(
            Long id,
            String tipo,
            String titulo,
            String cuerpo,
            Map<String, Object> datos,
            String ruta,
            String creadoEn,
            boolean leido) {
        static AvisoResponse de(AvisoBandeja a) {
            return new AvisoResponse(
                    a.id(),
                    a.tipo() == null ? null : a.tipo().name(),
                    a.titulo(),
                    a.cuerpo(),
                    a.datos(),
                    a.ruta(),
                    a.creadoEn().toString(),
                    a.leido());
        }
    }

    public record BandejaResponse(int noLeidos, List<AvisoResponse> avisos) {}

    private final BandejaAvisosUseCase bandeja;

    public BandejaAvisosController(BandejaAvisosUseCase bandeja) {
        this.bandeja = bandeja;
    }

    /** Los 30 más recientes y cuántos hay sin leer */
    @GetMapping
    public ResponseEntity<BandejaResponse> listar(Authentication auth) {
        Long id = CurrentUser.id(auth);
        return ResponseEntity.ok(new BandejaResponse(
                bandeja.noLeidos(id),
                bandeja.recientes(id).stream().map(AvisoResponse::de).toList()));
    }

    /** Solo el número del globito de la campana (se consulta seguido, por eso va aparte y es liviano) */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> noLeidos(Authentication auth) {
        return ResponseEntity.ok(Map.of("noLeidos", bandeja.noLeidos(CurrentUser.id(auth))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> marcarLeido(@PathVariable Long id, Authentication auth) {
        bandeja.marcarLeido(CurrentUser.id(auth), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> marcarTodos(Authentication auth) {
        bandeja.marcarTodosLeidos(CurrentUser.id(auth));
        return ResponseEntity.noContent().build();
    }
}
