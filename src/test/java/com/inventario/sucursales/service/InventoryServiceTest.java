package com.inventario.sucursales.service;

import com.inventario.sucursales.dto.InventoryAlertDto;
import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.entity.Inventory;
import com.inventario.sucursales.entity.Product;
import com.inventario.sucursales.repository.BranchRepository;
import com.inventario.sucursales.repository.InventoryRepository;
import com.inventario.sucursales.repository.InventoryRequestRepository;
import com.inventario.sucursales.repository.ProductRepository;
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
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRequestRepository requestRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Branch branch;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        branch = new Branch(1L, "SUC-01", "CDMX Norte", "CDMX", "CDMX", "Dir", "555", true);
        product = new Product(1L, "PROD-01", "Laptop", "Desc", "Electrónica", new BigDecimal("15000"), "Pza", 10, true);
        inventory = new Inventory(1L, branch, product, 5, LocalDateTime.now());
    }

    @Test
    void testGetInventoryByBranch() {
        when(inventoryRepository.findByBranchId(1L)).thenReturn(List.of(inventory));

        List<Inventory> result = inventoryService.getInventoryByBranch(1L);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getQuantity());
    }

    @Test
    void testGetAllInventory() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        List<Inventory> result = inventoryService.getAllInventory();

        assertEquals(1, result.size());
    }

    @Test
    void testGetLowStockAlerts() {
        Inventory criticalInv = new Inventory(2L, branch, product, 0, LocalDateTime.now());
        when(inventoryRepository.findLowStockByBranchId(1L)).thenReturn(List.of(inventory, criticalInv));

        List<InventoryAlertDto> alerts = inventoryService.getLowStockAlerts(1L);

        assertEquals(2, alerts.size());
        assertEquals("WARNING", alerts.get(0).getAlertLevel());
        assertEquals("CRITICAL", alerts.get(1).getAlertLevel());
    }

    @Test
    void testGetAllLowStockAlerts() {
        when(inventoryRepository.findAllLowStock()).thenReturn(List.of(inventory));

        List<InventoryAlertDto> alerts = inventoryService.getAllLowStockAlerts();

        assertEquals(1, alerts.size());
    }

    @Test
    void testUpdateStockSuccess() {
        when(inventoryRepository.findByBranchIdAndProductId(1L, 1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        Inventory updated = inventoryService.updateStock(1L, 1L, 20);

        assertEquals(20, updated.getQuantity());
    }

    @Test
    void testUpdateStockNegativeFails() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.updateStock(1L, 1L, -5));
    }

    @Test
    void testAdjustStockIncrement() {
        when(inventoryRepository.findByBranchIdAndProductId(1L, 1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        Inventory result = inventoryService.adjustStock(1L, 1L, 10);

        assertEquals(15, result.getQuantity());
    }

    @Test
    void testAdjustStockDecrementSuccess() {
        when(inventoryRepository.findByBranchIdAndProductId(1L, 1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        Inventory result = inventoryService.adjustStock(1L, 1L, -3);

        assertEquals(2, result.getQuantity());
    }

    @Test
    void testAdjustStockInsufficientThrows() {
        when(inventoryRepository.findByBranchIdAndProductId(1L, 1L)).thenReturn(Optional.of(inventory));

        assertThrows(IllegalStateException.class, () -> inventoryService.adjustStock(1L, 1L, -10));
    }

    @Test
    void testFindOrCreateInventoryWhenNotExists() {
        when(inventoryRepository.findByBranchIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        Inventory created = inventoryService.findOrCreateInventory(1L, 1L);

        assertNotNull(created);
        assertEquals(0, created.getQuantity());
    }
}
