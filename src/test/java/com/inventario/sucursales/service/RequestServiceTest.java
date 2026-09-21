package com.inventario.sucursales.service;

import com.inventario.sucursales.dto.CreateRequestDto;
import com.inventario.sucursales.dto.ReviewRequestDto;
import com.inventario.sucursales.entity.*;
import com.inventario.sucursales.repository.BranchRepository;
import com.inventario.sucursales.repository.InventoryRequestRepository;
import com.inventario.sucursales.repository.ProductRepository;
import com.inventario.sucursales.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private InventoryRequestRepository requestRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private RequestService requestService;

    private Branch originBranch;
    private Branch destBranch;
    private Product product;
    private User adminUser;
    private User gerenteUser;
    private Inventory originStock;

    @BeforeEach
    void setUp() {
        originBranch = new Branch(1L, "SUC-01", "CDMX Norte", "CDMX", "CDMX", "Dir", "555", true);
        destBranch = new Branch(2L, "SUC-02", "CDMX Sur", "CDMX", "CDMX", "Dir", "555", true);
        product = new Product(10L, "PROD-01", "Laptop", "Desc", "Electrónica", new BigDecimal("15000"), "Pza", 5, true);

        adminUser = new User(1L, "admin", "pass", "Admin User", "a@test.com", Role.ROLE_ADMIN, null, "Dir", "123", true);
        gerenteUser = new User(2L, "gerente_cdmx", "pass", "Gerente", "g@test.com", Role.ROLE_GERENTE, destBranch, "Gerente", "123", true);

        originStock = new Inventory(1L, originBranch, product, 20, LocalDateTime.now());
    }

    @Test
    void testGetAllRequestsAsAdmin() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(requestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(new InventoryRequest()));

        List<InventoryRequest> list = requestService.getAllRequests("admin");

        assertEquals(1, list.size());
        verify(requestRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void testGetAllRequestsAsGerente() {
        when(userRepository.findByUsername("gerente_cdmx")).thenReturn(Optional.of(gerenteUser));
        when(requestRepository.findByBranchInvolved(2L)).thenReturn(List.of(new InventoryRequest()));

        List<InventoryRequest> list = requestService.getAllRequests("gerente_cdmx");

        assertEquals(1, list.size());
        verify(requestRepository).findByBranchInvolved(2L);
    }

    @Test
    void testCreateTransferRequestSuccess() {
        CreateRequestDto dto = new CreateRequestDto(RequestType.TRANSFER, 1L, 2L, 10L, 5, "Falta stock");

        when(userRepository.findByUsername("gerente_cdmx")).thenReturn(Optional.of(gerenteUser));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(destBranch));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(originBranch));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryService.getInventoryByBranchAndProduct(1L, 10L)).thenReturn(originStock);
        when(requestRepository.save(any(InventoryRequest.class))).thenAnswer(i -> i.getArgument(0));

        InventoryRequest created = requestService.createRequest(dto, "gerente_cdmx");

        assertNotNull(created);
        assertEquals(RequestStatus.PENDING, created.getStatus());
        assertEquals(5, created.getQuantity());
        assertEquals(RequestType.TRANSFER, created.getRequestType());
    }

    @Test
    void testCreateTransferSameOriginAndDestinationThrows() {
        CreateRequestDto dto = new CreateRequestDto(RequestType.TRANSFER, 2L, 2L, 10L, 5, "Misma sucursal");

        when(userRepository.findByUsername("gerente_cdmx")).thenReturn(Optional.of(gerenteUser));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(destBranch));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> requestService.createRequest(dto, "gerente_cdmx"));
    }

    @Test
    void testCreateTransferInsufficientOriginStockThrows() {
        CreateRequestDto dto = new CreateRequestDto(RequestType.TRANSFER, 1L, 2L, 10L, 50, "Excede stock");

        when(userRepository.findByUsername("gerente_cdmx")).thenReturn(Optional.of(gerenteUser));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(destBranch));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(originBranch));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryService.getInventoryByBranchAndProduct(1L, 10L)).thenReturn(originStock); // Stock 20 < 50

        assertThrows(IllegalStateException.class, () -> requestService.createRequest(dto, "gerente_cdmx"));
    }

    @Test
    void testCreateSupplierRequestSuccess() {
        CreateRequestDto dto = new CreateRequestDto(RequestType.SUPPLIER, null, 2L, 10L, 30, "Pedido a proveedor");

        when(userRepository.findByUsername("gerente_cdmx")).thenReturn(Optional.of(gerenteUser));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(destBranch));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(requestRepository.save(any(InventoryRequest.class))).thenAnswer(i -> i.getArgument(0));

        InventoryRequest created = requestService.createRequest(dto, "gerente_cdmx");

        assertNotNull(created);
        assertNull(created.getOriginBranch());
        assertEquals(RequestType.SUPPLIER, created.getRequestType());
    }

    @Test
    void testApproveTransferRequestSuccess() {
        InventoryRequest req = new InventoryRequest(100L, RequestType.TRANSFER, originBranch, destBranch, product, 5, RequestStatus.PENDING, gerenteUser, null, "Nota", null, LocalDateTime.now(), null);

        when(requestRepository.findById(100L)).thenReturn(Optional.of(req));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(requestRepository.save(any(InventoryRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReviewRequestDto review = new ReviewRequestDto("Aprobado sin problemas");
        InventoryRequest approved = requestService.approveRequest(100L, review, "admin");

        assertEquals(RequestStatus.APPROVED, approved.getStatus());
        assertEquals("Aprobado sin problemas", approved.getAdminComments());
        verify(inventoryService).adjustStock(1L, 10L, -5);
        verify(inventoryService).adjustStock(2L, 10L, 5);
    }

    @Test
    void testApproveSupplierRequestSuccess() {
        InventoryRequest req = new InventoryRequest(101L, RequestType.SUPPLIER, null, destBranch, product, 20, RequestStatus.PENDING, gerenteUser, null, "Nota", null, LocalDateTime.now(), null);

        when(requestRepository.findById(101L)).thenReturn(Optional.of(req));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(requestRepository.save(any(InventoryRequest.class))).thenAnswer(i -> i.getArgument(0));

        InventoryRequest approved = requestService.approveRequest(101L, null, "admin");

        assertEquals(RequestStatus.APPROVED, approved.getStatus());
        verify(inventoryService).adjustStock(2L, 10L, 20);
    }

    @Test
    void testRejectRequestSuccess() {
        InventoryRequest req = new InventoryRequest(102L, RequestType.TRANSFER, originBranch, destBranch, product, 5, RequestStatus.PENDING, gerenteUser, null, "Nota", null, LocalDateTime.now(), null);

        when(requestRepository.findById(102L)).thenReturn(Optional.of(req));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(requestRepository.save(any(InventoryRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReviewRequestDto review = new ReviewRequestDto("Rechazado por falta de camión");
        InventoryRequest rejected = requestService.rejectRequest(102L, review, "admin");

        assertEquals(RequestStatus.REJECTED, rejected.getStatus());
        assertEquals("Rechazado por falta de camión", rejected.getAdminComments());
    }

    @Test
    void testApproveAlreadyApprovedThrows() {
        InventoryRequest req = new InventoryRequest(103L, RequestType.TRANSFER, originBranch, destBranch, product, 5, RequestStatus.APPROVED, gerenteUser, adminUser, "Nota", "Ok", LocalDateTime.now(), LocalDateTime.now());

        when(requestRepository.findById(103L)).thenReturn(Optional.of(req));

        assertThrows(IllegalStateException.class, () -> requestService.approveRequest(103L, null, "admin"));
    }
}
