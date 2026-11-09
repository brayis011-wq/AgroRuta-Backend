package com.agroruta.admin.infrastructure.web;

import com.agroruta.admin.application.ports.in.AdministrarSistemaUseCase;
import com.agroruta.admin.infrastructure.web.dto.AgricultorAdminResponse;
import com.agroruta.admin.infrastructure.web.dto.AgricultorDetalleResponse;
import com.agroruta.admin.infrastructure.web.dto.CambiarEstadoUsuarioRequest;
import com.agroruta.admin.infrastructure.web.dto.CambiarRolUsuarioRequest;
import com.agroruta.admin.infrastructure.web.dto.ResumenAdminResponse;
import com.agroruta.admin.infrastructure.web.dto.UsuarioAdminResponse;
import com.agroruta.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Panel de administración. Todo /api/admin/** es solo para el rol Administrador. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('Administrador')")
public class AdminController {

    private final AdministrarSistemaUseCase adminUseCase;

    public AdminController(AdministrarSistemaUseCase adminUseCase) {
        this.adminUseCase = adminUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<ResumenAdminResponse> resumen() {
        return ResponseEntity.ok(ResumenAdminResponse.from(adminUseCase.resumen()));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UsuarioAdminResponse>> usuarios() {
        return ResponseEntity.ok(adminUseCase.listarUsuarios().stream()
                .map(UsuarioAdminResponse::from).toList());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UsuarioAdminResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(UsuarioAdminResponse.from(
                adminUseCase.cambiarEstado(CurrentUser.id(authentication), id, request.activo())));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UsuarioAdminResponse> cambiarRol(
            @PathVariable Long id,
            @Valid @RequestBody CambiarRolUsuarioRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(UsuarioAdminResponse.from(
                adminUseCase.cambiarRol(CurrentUser.id(authentication), id, request.rol())));
    }

    @GetMapping("/farmers")
    public ResponseEntity<List<AgricultorAdminResponse>> agricultores() {
        return ResponseEntity.ok(adminUseCase.listarAgricultores().stream()
                .map(AgricultorAdminResponse::from).toList());
    }

    @GetMapping("/farmers/{id}")
    public ResponseEntity<AgricultorDetalleResponse> agricultor(@PathVariable Long id) {
        return ResponseEntity.ok(AgricultorDetalleResponse.from(adminUseCase.detalleAgricultor(id)));
    }
}
