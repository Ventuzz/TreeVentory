package com.inventario.sucursales.service;

import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// # Servicio de operaciones CRUD y consultas para el catálogo de sucursales
@Service
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada con id: " + id));
    }

    @Transactional
    public Branch createBranch(Branch branch) {
        if (branchRepository.findByCode(branch.getCode()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una sucursal con el codigo: " + branch.getCode());
        }
        branch.setActive(true);
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(Long id, Branch updated) {
        Branch branch = getBranchById(id);
        branch.setName(updated.getName());
        branch.setCity(updated.getCity());
        branch.setState(updated.getState());
        branch.setAddress(updated.getAddress());
        branch.setPhone(updated.getPhone());
        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = getBranchById(id);
        branch.setActive(false);
        branchRepository.save(branch);
    }
}
