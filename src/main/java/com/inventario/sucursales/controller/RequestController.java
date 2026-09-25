package com.inventario.sucursales.controller;

import com.inventario.sucursales.dto.ApiResponse;
import com.inventario.sucursales.dto.CreateRequestDto;
import com.inventario.sucursales.dto.ReviewRequestDto;
import com.inventario.sucursales.entity.InventoryRequest;
import com.inventario.sucursales.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador REST para emisión, consulta y resolución de solicitudes de inventario
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryRequest>>> getAllRequests(Authentication authentication) {
        String username = authentication.getName();
        List<InventoryRequest> requests = requestService.getAllRequests(username);
        return ResponseEntity.ok(ApiResponse.success("Solicitudes obtenidas con exito", requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryRequest>> getRequestById(@PathVariable Long id) {
        InventoryRequest request = requestService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.success("Solicitud encontrada", request));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryRequest>> createRequest(
            @Valid @RequestBody CreateRequestDto dto,
            Authentication authentication) {
        String username = authentication.getName();
        InventoryRequest created = requestService.createRequest(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Solicitud creada exitosamente en estado PENDIENTE", created));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryRequest>> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewRequestDto reviewDto,
            Authentication authentication) {
        String adminUsername = authentication.getName();
        InventoryRequest approved = requestService.approveRequest(id, reviewDto, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Solicitud aprobada y stock actualizado exitosamente", approved));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryRequest>> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewRequestDto reviewDto,
            Authentication authentication) {
        String adminUsername = authentication.getName();
        InventoryRequest rejected = requestService.rejectRequest(id, reviewDto, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Solicitud rechazada exitosamente", rejected));
    }
}
