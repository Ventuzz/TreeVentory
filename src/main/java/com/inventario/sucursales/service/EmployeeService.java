package com.inventario.sucursales.service;

import com.inventario.sucursales.dto.EmployeeDto;
import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.entity.Role;
import com.inventario.sucursales.entity.User;
import com.inventario.sucursales.repository.BranchRepository;
import com.inventario.sucursales.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(UserRepository userRepository, BranchRepository branchRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> getAllEmployees() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> getEmployeesByBranch(Long branchId) {
        return userRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public User getEmployeeById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con id: " + id));
    }

    @Transactional
    public User createEmployee(EmployeeDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario '" + dto.getUsername() + "' ya esta registrado");
        }

        Branch branch = null;
        if (dto.getBranchId() != null) {
            branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Sucursal no valida: " + dto.getBranchId()));
        } else if (dto.getRole() == Role.ROLE_GERENTE) {
            throw new IllegalArgumentException("Un gerente de sucursal debe tener obligatoriamente una sucursal asignada");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword() != null && !dto.getPassword().isBlank() ? dto.getPassword() : "empleado123"));
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setBranch(branch);
        user.setPosition(dto.getPosition());
        user.setPhone(dto.getPhone());
        user.setActive(dto.isActive());

        return userRepository.save(user);
    }

    @Transactional
    public User updateEmployee(Long id, EmployeeDto dto) {
        User user = getEmployeeById(id);

        if (!user.getUsername().equals(dto.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario '" + dto.getUsername() + "' ya esta en uso");
        }

        Branch branch = null;
        if (dto.getBranchId() != null) {
            branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Sucursal no valida: " + dto.getBranchId()));
        } else if (dto.getRole() == Role.ROLE_GERENTE) {
            throw new IllegalArgumentException("Un gerente de sucursal debe tener una sucursal asignada");
        }

        user.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setBranch(branch);
        user.setPosition(dto.getPosition());
        user.setPhone(dto.getPhone());
        user.setActive(dto.isActive());

        return userRepository.save(user);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        User user = getEmployeeById(id);
        user.setActive(false);
        userRepository.save(user);
    }
}
