package com.inventario.sucursales.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventario.sucursales.dto.*;
import com.inventario.sucursales.entity.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// # Pruebas unitarias de cobertura y validaciones de controladores REST
@SpringBootTest
@AutoConfigureMockMvc
class ControllerCoverageTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

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
        private Product sampleProduct;

        @BeforeEach
        void setUp() {
                sampleBranch = new Branch(1L, "SUC-01", "CDMX Norte", "CDMX", "CDMX", "Dir", "555", true);
                sampleProduct = new Product(1L, "PROD-01", "Laptop Pro", "Desc", "Electrónica", new BigDecimal("15000"),
                                "Pza", 5, true);
        }

        // --- BranchController Tests ---
        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testBranchControllerEndpoints() throws Exception {
                when(branchService.getBranchById(1L)).thenReturn(sampleBranch);
                when(branchService.createBranch(any(Branch.class))).thenReturn(sampleBranch);
                when(branchService.updateBranch(eq(1L), any(Branch.class))).thenReturn(sampleBranch);

                mockMvc.perform(get("/api/branches/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                mockMvc.perform(post("/api/branches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleBranch)))
                                .andExpect(status().isCreated());

                mockMvc.perform(put("/api/branches/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleBranch)))
                                .andExpect(status().isOk());

                mockMvc.perform(delete("/api/branches/1"))
                                .andExpect(status().isOk());
                verify(branchService).deleteBranch(1L);
        }

        // --- ProductController Tests ---
        @Test
        @WithMockUser(username = "gerente_cdmx", roles = { "GERENTE" })
        void testProductControllerReadAndWrite() throws Exception {
                when(productService.getAllProducts()).thenReturn(List.of(sampleProduct));
                when(productService.getProductById(1L)).thenReturn(sampleProduct);
                when(productService.getAllCategories()).thenReturn(List.of("Electrónica", "Hogar"));
                when(productService.createProduct(any(Product.class))).thenReturn(sampleProduct);
                when(productService.updateProduct(eq(1L), any(Product.class))).thenReturn(sampleProduct);

                mockMvc.perform(get("/api/products"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/products/1"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/products/categories"))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleProduct)))
                                .andExpect(status().isCreated());

                mockMvc.perform(put("/api/products/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleProduct)))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testProductControllerDeleteAdmin() throws Exception {
                mockMvc.perform(delete("/api/products/1"))
                                .andExpect(status().isOk());
                verify(productService).deleteProduct(1L);
        }

        // --- InventoryController Tests ---
        @Test
        @WithMockUser(username = "gerente_cdmx", roles = { "GERENTE" })
        void testInventoryControllerRead() throws Exception {
                Inventory inv = new Inventory(1L, sampleBranch, sampleProduct, 8, LocalDateTime.now());
                InventoryAlertDto alert = new InventoryAlertDto(1L, 1L, "CDMX Norte", 1L, "Laptop Pro", "PROD-01",
                                "Electrónica", 3, 5);

                when(inventoryService.getInventoryByBranch(1L)).thenReturn(List.of(inv));
                when(inventoryService.getAllInventory()).thenReturn(List.of(inv));
                when(inventoryService.getLowStockAlerts(1L)).thenReturn(List.of(alert));
                when(inventoryService.getAllLowStockAlerts()).thenReturn(List.of(alert));

                mockMvc.perform(get("/api/inventory/branch/1"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/inventory/all"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/inventory/alerts/1"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/inventory/alerts"))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testInventoryControllerUpdateStockAdmin() throws Exception {
                Inventory inv = new Inventory(1L, sampleBranch, sampleProduct, 50, LocalDateTime.now());
                when(inventoryService.updateStock(1L, 1L, 50)).thenReturn(inv);

                mockMvc.perform(put("/api/inventory/branch/1/product/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quantity\":50}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.quantity").value(50));
        }

        // --- RequestController Tests ---
        @Test
        @WithMockUser(username = "gerente_cdmx", roles = { "GERENTE" })
        void testRequestControllerReadAndCreate() throws Exception {
                User reqUser = new User(2L, "gerente_cdmx", "pass", "Ana", "a@test.com", Role.ROLE_GERENTE,
                                sampleBranch, "Gerente", "123", true);
                InventoryRequest req = new InventoryRequest(1L, RequestType.TRANSFER, null, sampleBranch, sampleProduct,
                                10, RequestStatus.PENDING, reqUser, null, "Nota", null, LocalDateTime.now(), null);
                CreateRequestDto dto = new CreateRequestDto(RequestType.SUPPLIER, null, 1L, 1L, 10, "Urgente");

                when(requestService.getAllRequests("gerente_cdmx")).thenReturn(List.of(req));
                when(requestService.getRequestById(1L)).thenReturn(req);
                when(requestService.createRequest(any(CreateRequestDto.class), eq("gerente_cdmx"))).thenReturn(req);

                mockMvc.perform(get("/api/requests"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/requests/1"))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testRequestControllerApproveAndRejectAdmin() throws Exception {
                User reqUser = new User(2L, "gerente_cdmx", "pass", "Ana", "a@test.com", Role.ROLE_GERENTE,
                                sampleBranch, "Gerente", "123", true);
                InventoryRequest req = new InventoryRequest(1L, RequestType.TRANSFER, null, sampleBranch, sampleProduct,
                                10, RequestStatus.APPROVED, reqUser, null, "Nota", "Ok", LocalDateTime.now(),
                                LocalDateTime.now());

                when(requestService.approveRequest(eq(1L), any(), eq("admin"))).thenReturn(req);
                when(requestService.rejectRequest(eq(1L), any(), eq("admin"))).thenReturn(req);

                mockMvc.perform(put("/api/requests/1/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"adminComments\":\"Aprobado\"}"))
                                .andExpect(status().isOk());

                mockMvc.perform(put("/api/requests/1/reject")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"adminComments\":\"Rechazado\"}"))
                                .andExpect(status().isOk());
        }

        // --- EmployeeController Tests ---
        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testEmployeeControllerCrudAdmin() throws Exception {
                User emp = new User(1L, "emp1", "pass", "Empleado 1", "e@test.com", Role.ROLE_GERENTE, sampleBranch,
                                "Puesto", "123", true);
                EmployeeDto dto = new EmployeeDto(null, "emp1", "pass", "Empleado 1", "e@test.com", Role.ROLE_GERENTE,
                                1L, "Puesto", "123", true);

                when(employeeService.getAllEmployees()).thenReturn(List.of(emp));
                when(employeeService.getEmployeesByBranch(1L)).thenReturn(List.of(emp));
                when(employeeService.getEmployeeById(1L)).thenReturn(emp);
                when(employeeService.createEmployee(any(EmployeeDto.class))).thenReturn(emp);
                when(employeeService.updateEmployee(eq(1L), any(EmployeeDto.class))).thenReturn(emp);

                mockMvc.perform(get("/api/employees"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/employees/branch/1"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/employees/1"))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/employees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated());

                mockMvc.perform(put("/api/employees/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk());

                mockMvc.perform(delete("/api/employees/1"))
                                .andExpect(status().isOk());
                verify(employeeService).deleteEmployee(1L);
        }
}
