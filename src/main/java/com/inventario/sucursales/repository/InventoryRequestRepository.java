package com.inventario.sucursales.repository;

import com.inventario.sucursales.entity.InventoryRequest;
import com.inventario.sucursales.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRequestRepository extends JpaRepository<InventoryRequest, Long> {

    List<InventoryRequest> findAllByOrderByCreatedAtDesc();

    List<InventoryRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<InventoryRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    @Query("SELECT r FROM InventoryRequest r WHERE r.destinationBranch.id = :branchId OR r.originBranch.id = :branchId ORDER BY r.createdAt DESC")
    List<InventoryRequest> findByBranchInvolved(@Param("branchId") Long branchId);
}
