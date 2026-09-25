package com.inventario.sucursales.controller;

import com.inventario.sucursales.dto.ApiResponse;
import com.inventario.sucursales.dto.InventoryAlertDto;
import com.inventario.sucursales.entity.Inventory;
import com.inventario.sucursales.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Controlador REST para consulta de existencias por sucursal, matriz global y alertas de stock
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<Inventory>>> getInventoryByBranch(@PathVariable Long branchId) {
        List<Inventory> inventoryList = inventoryService.getInventoryByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Inventario de la sucursal obtenido con exito", inventoryList));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Inventory>>> getAllInventory() {
        List<Inventory> allInventory = inventoryService.getAllInventory();
        return ResponseEntity.ok(ApiResponse.success("Inventario global obtenido con exito", allInventory));
    }

    @GetMapping("/alerts/{branchId}")
    public ResponseEntity<ApiResponse<List<InventoryAlertDto>>> getLowStockAlerts(@PathVariable Long branchId) {
        List<InventoryAlertDto> alerts = inventoryService.getLowStockAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Alertas de inventario bajo obtenidas", alerts));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<InventoryAlertDto>>> getAllLowStockAlerts() {
        List<InventoryAlertDto> alerts = inventoryService.getAllLowStockAlerts();
        return ResponseEntity.ok(ApiResponse.success("Alertas globales de inventario bajo obtenidas", alerts));
    }

    @PutMapping("/branch/{branchId}/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Inventory>> updateStock(
            @PathVariable Long branchId,
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> payload) {
        Integer quantity = payload.get("quantity");
        if (quantity == null) {
            throw new IllegalArgumentException("El campo 'quantity' es obligatorio");
        }
        Inventory updated = inventoryService.updateStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock actualizado exitosamente", updated));
    }
}
