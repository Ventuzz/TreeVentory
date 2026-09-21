package com.inventario.sucursales.service;

import com.inventario.sucursales.dto.InventoryAlertDto;
import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.entity.Inventory;
import com.inventario.sucursales.entity.Product;
import com.inventario.sucursales.repository.BranchRepository;
import com.inventario.sucursales.repository.InventoryRepository;
import com.inventario.sucursales.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, BranchRepository branchRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Inventory> getInventoryByBranch(Long branchId) {
        return inventoryRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Inventory getInventoryByBranchAndProduct(Long branchId, Long productId) {
        return inventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<InventoryAlertDto> getLowStockAlerts(Long branchId) {
        List<Inventory> lowStockList = inventoryRepository.findLowStockByBranchId(branchId);
        return lowStockList.stream()
                .map(this::mapToAlertDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryAlertDto> getAllLowStockAlerts() {
        List<Inventory> lowStockList = inventoryRepository.findAllLowStock();
        return lowStockList.stream()
                .map(this::mapToAlertDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Inventory updateStock(Long branchId, Long productId, Integer newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        Inventory inventory = findOrCreateInventory(branchId, productId);
        inventory.setQuantity(newQuantity);
        inventory.setLastUpdated(LocalDateTime.now());
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory adjustStock(Long branchId, Long productId, int delta) {
        Inventory inventory = findOrCreateInventory(branchId, productId);
        int finalQuantity = inventory.getQuantity() + delta;
        if (finalQuantity < 0) {
            throw new IllegalStateException("Stock insuficiente en la sucursal: " + inventory.getBranch().getName()
                    + " para el producto: " + inventory.getProduct().getName()
                    + ". Stock disponible: " + inventory.getQuantity() + ", requerido: " + Math.abs(delta));
        }
        inventory.setQuantity(finalQuantity);
        inventory.setLastUpdated(LocalDateTime.now());
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory findOrCreateInventory(Long branchId, Long productId) {
        return inventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseGet(() -> {
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada: " + branchId));
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));
                    Inventory newInv = new Inventory();
                    newInv.setBranch(branch);
                    newInv.setProduct(product);
                    newInv.setQuantity(0);
                    newInv.setLastUpdated(LocalDateTime.now());
                    return inventoryRepository.save(newInv);
                });
    }

    private InventoryAlertDto mapToAlertDto(Inventory inv) {
        return new InventoryAlertDto(
                inv.getId(),
                inv.getBranch().getId(),
                inv.getBranch().getName(),
                inv.getProduct().getId(),
                inv.getProduct().getName(),
                inv.getProduct().getSku(),
                inv.getProduct().getCategory(),
                inv.getQuantity(),
                inv.getProduct().getMinStockThreshold()
        );
    }
}
