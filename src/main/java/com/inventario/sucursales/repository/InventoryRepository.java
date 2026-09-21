package com.inventario.sucursales.repository;

import com.inventario.sucursales.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// # Repositorio JPA para persistencia de inventario y detección de existencias bajo umbral
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByBranchId(Long branchId);
    Optional<Inventory> findByBranchIdAndProductId(Long branchId, Long productId);
    List<Inventory> findByProductId(Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.branch.id = :branchId AND i.quantity <= i.product.minStockThreshold")
    List<Inventory> findLowStockByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.product.minStockThreshold")
    List<Inventory> findAllLowStock();
}
