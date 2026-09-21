package com.inventario.sucursales.config;

import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.entity.Role;
import com.inventario.sucursales.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "SuperClaveSecretaParaPruebasUnitariasJUnit5JaCoCo2026DebeTenerMasDe256BitsLongitud");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000L);
    }

    @Test
    void testGenerateAndValidateToken() {
        Branch branch = new Branch(1L, "SUC-01", "Sucursal CDMX", "CDMX", "CDMX", "Dir", "123", true);
        User user = new User(10L, "gerente_cdmx", "pass", "Ana Morales", "ana@test.com", Role.ROLE_GERENTE, branch, "Gerente", "123", true);

        String token = jwtUtils.generateToken(user);

        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
        assertEquals("gerente_cdmx", jwtUtils.getUsernameFromToken(token));
        assertEquals("ROLE_GERENTE", jwtUtils.getRoleFromToken(token));
        assertEquals(1L, jwtUtils.getBranchIdFromToken(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
        assertFalse(jwtUtils.validateToken(""));
        assertFalse(jwtUtils.validateToken(null));
    }
}
