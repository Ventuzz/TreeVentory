package com.inventario.sucursales.service;

import com.inventario.sucursales.config.JwtUtils;
import com.inventario.sucursales.dto.LoginRequest;
import com.inventario.sucursales.dto.LoginResponse;
import com.inventario.sucursales.entity.User;
import com.inventario.sucursales.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// # Servicio de lógica de negocio para autenticación, verificación de credenciales y generación de JWT
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public LoginResponse authenticate(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas: usuario no encontrado"));

        if (!user.isActive()) {
            throw new BadCredentialsException("El usuario se encuentra inactivo en el sistema");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas: contrasena invalida");
        }

        String token = jwtUtils.generateToken(user);
        Long branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        String branchName = user.getBranch() != null ? user.getBranch().getName() : "Acceso Global (Corporativo)";

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                branchId,
                branchName
        );
    }
}
