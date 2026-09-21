package com.inventario.sucursales.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventario.sucursales.config.JwtUtils;
import com.inventario.sucursales.dto.LoginRequest;
import com.inventario.sucursales.dto.LoginResponse;
import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.entity.Role;
import com.inventario.sucursales.entity.User;
import com.inventario.sucursales.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private BranchService branchService;

    @MockBean
    private ProductService productService;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private RequestService requestService;

    @MockBean
    private EmployeeService employeeService;

    private Branch sampleBranch;

    @BeforeEach
    void setUp() {
        sampleBranch = new Branch(1L, "SUC-01", "CDMX Norte", "CDMX", "CDMX", "Dir", "555", true);
    }

    // 1. Validar que un usuario SIN TOKEN no pueda acceder a endpoints protegidos (401 Unauthorized)
    @Test
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/inventory/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No autorizado")));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isUnauthorized());
    }

    // 2. Operación restringida EXCLUSIVAMENTE a ADMIN: Gerente intentando acceder recibe 403 Forbidden
    @Test
    @WithMockUser(username = "gerente_cdmx", roles = {"GERENTE"})
    void testGerenteAccessToAdminOnlyEmployeesReturns403() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Acceso denegado")));
    }

    @Test
    @WithMockUser(username = "gerente_cdmx", roles = {"GERENTE"})
    void testGerenteAccessToApproveRequestReturns403() throws Exception {
        mockMvc.perform(put("/api/requests/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminComments\":\"intento\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gerente_cdmx", roles = {"GERENTE"})
    void testGerenteAccessToDeleteProductReturns403() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isForbidden());
    }

    // 3. Operación disponible para usuarios autenticados (ADMIN y GERENTE): Ambos pueden consultar sucursales y productos
    @Test
    @WithMockUser(username = "gerente_cdmx", roles = {"GERENTE"})
    void testGerenteCanAccessBranchesReturns200() throws Exception {
        when(branchService.getAllBranches()).thenReturn(List.of(sampleBranch));

        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("SUC-01"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCanAccessBranchesReturns200() throws Exception {
        when(branchService.getAllBranches()).thenReturn(List.of(sampleBranch));

        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // 4. Operación exclusiva de ADMIN ejecutada con rol ADMIN -> 200 OK
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminAccessToEmployeesReturns200() throws Exception {
        User emp = new User(1L, "emp1", "pass", "Luis Perez", "l@test.com", Role.ROLE_GERENTE, sampleBranch, "Puesto", "123", true);
        when(employeeService.getAllEmployees()).thenReturn(List.of(emp));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("emp1"));
    }

    // 5. Endpoint público de autenticación login
    @Test
    void testPublicLoginEndpointReturns200() throws Exception {
        LoginRequest req = new LoginRequest("admin", "admin123");
        LoginResponse resp = new LoginResponse("mock.jwt.token", "admin", "Carlos Mendoza", "ROLE_ADMIN", 1L, "CDMX");
        when(authService.authenticate(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }
}
