package com.inventario.sucursales.controller;

import com.inventario.sucursales.dto.ApiResponse;
import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// # Controlador REST para consulta y administración de sucursales
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Branch>>> getAllBranches() {
        List<Branch> branches = branchService.getAllBranches();
        return ResponseEntity.ok(ApiResponse.success("Sucursales obtenidas con exito", branches));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Branch>> getBranchById(@PathVariable Long id) {
        Branch branch = branchService.getBranchById(id);
        return ResponseEntity.ok(ApiResponse.success("Sucursal encontrada", branch));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Branch>> createBranch(@Valid @RequestBody Branch branch) {
        Branch created = branchService.createBranch(branch);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sucursal creada exitosamente", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Branch>> updateBranch(@PathVariable Long id, @Valid @RequestBody Branch branch) {
        Branch updated = branchService.updateBranch(id, branch);
        return ResponseEntity.ok(ApiResponse.success("Sucursal actualizada exitosamente", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success("Sucursal eliminada exitosamente"));
    }
}
