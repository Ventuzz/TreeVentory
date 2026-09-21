package com.inventario.sucursales.repository;

import com.inventario.sucursales.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// # Repositorio JPA para acceso a datos y consultas sobre la entidad Branch
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByCode(String code);
    List<Branch> findByActiveTrueOrderByNameAsc();
}
