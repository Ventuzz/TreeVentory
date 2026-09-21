package com.inventario.sucursales.service;

import com.inventario.sucursales.config.JwtUtils;
import com.inventario.sucursales.dto.LoginRequest;
import com.inventario.sucursales.dto.LoginResponse;
import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.entity.Role;
import com.inventario.sucursales.entity.User;
import com.inventario.sucursales.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Branch sampleBranch;

    @BeforeEach
    void setUp() {
        sampleBranch = new Branch(1L, "SUC-01", "CDMX", "CDMX", "CDMX", "Av", "555", true);
        sampleUser = new User(1L, "admin", "encodedPass", "Carlos Mendoza", "admin@test.com", Role.ROLE_ADMIN, sampleBranch, "Director", "555", true);
    }

    @Test
    void testAuthenticateSuccess() {
        LoginRequest req = new LoginRequest("admin", "admin123");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("admin123", "encodedPass")).thenReturn(true);
        when(jwtUtils.generateToken(sampleUser)).thenReturn("dummy.jwt.token");

        LoginResponse resp = authService.authenticate(req);

        assertNotNull(resp);
        assertEquals("dummy.jwt.token", resp.getToken());
        assertEquals("admin", resp.getUsername());
        assertEquals("ROLE_ADMIN", resp.getRole());
        assertEquals(1L, resp.getBranchId());
    }

    @Test
    void testAuthenticateUserNotFound() {
        LoginRequest req = new LoginRequest("inexistente", "123");
        when(userRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(req));
    }

    @Test
    void testAuthenticateInactiveUser() {
        sampleUser.setActive(false);
        LoginRequest req = new LoginRequest("admin", "admin123");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(req));
    }

    @Test
    void testAuthenticateBadPassword() {
        LoginRequest req = new LoginRequest("admin", "wrongPass");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPass", "encodedPass")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(req));
    }
}
