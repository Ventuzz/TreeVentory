package com.inventario.sucursales.repository;

import com.inventario.sucursales.entity.Role;
import com.inventario.sucursales.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repositorio JPA para gestión de cuentas de usuario, roles y pertenencia a sucursales
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    List<User> findByBranchId(Long branchId);

    List<User> findByRole(Role role);

    List<User> findByActiveTrue();

    boolean existsByUsername(String username);
}
