package cl.duoc.ventas.controller;

import cl.duoc.ventas.dto.ActualizarEstadoVentaRequest;
import cl.duoc.ventas.dto.CrearVentaRequest;
import cl.duoc.ventas.dto.VentaResponse;
import cl.duoc.ventas.model.EstadoVenta;
import cl.duoc.ventas.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    public ResponseEntity<VentaResponse> procesarCheckout(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @Valid @RequestBody CrearVentaRequest request
    ) {
        VentaResponse venta = ventaService.procesarCheckout(authentication.getName(), authHeader, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(venta);
    }

    @PostMapping("/rechazadas")
    public ResponseEntity<VentaResponse> registrarVentaRechazada(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @Valid @RequestBody CrearVentaRequest request
    ) {
        VentaResponse venta = ventaService.registrarVentaRechazada(authentication.getName(), authHeader, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(venta);
    }

    @GetMapping("/mis-compras")
    public ResponseEntity<List<VentaResponse>> listarMisCompras(Authentication authentication) {
        return ResponseEntity.ok(ventaService.listarMisCompras(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtenerPorId(Authentication authentication, @PathVariable Long id) {
        boolean esStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")
                        || a.getAuthority().equals("ROLE_TRABAJADOR"));
        return ResponseEntity.ok(ventaService.obtenerPorId(id, authentication.getName(), esStaff));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<List<VentaResponse>> listarTodas(@RequestParam(required = false) EstadoVenta estado) {
        return ResponseEntity.ok(ventaService.listarTodas(estado));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR')")
    public ResponseEntity<VentaResponse> actualizarEstado(@PathVariable Long id,
                                                          @Valid @RequestBody ActualizarEstadoVentaRequest request) {
        return ResponseEntity.ok(ventaService.actualizarEstado(id, request));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<VentaResponse> anularVenta(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        return ResponseEntity.ok(ventaService.anularVenta(id, authHeader));
    }
}
