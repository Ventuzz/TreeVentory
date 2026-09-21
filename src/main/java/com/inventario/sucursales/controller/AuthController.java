package com.inventario.sucursales.controller;

import com.inventario.sucursales.dto.ApiResponse;
import com.inventario.sucursales.dto.LoginRequest;
import com.inventario.sucursales.dto.LoginResponse;
import com.inventario.sucursales.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// # Controlador REST para autenticación y emisión de tokens JWT
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Inicio de sesion exitoso", response));
    }
}
