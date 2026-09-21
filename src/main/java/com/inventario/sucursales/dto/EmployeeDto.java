package com.inventario.sucursales.dto;

import com.inventario.sucursales.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EmployeeDto {

    private Long id;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    private String password; // Obligatorio solo al crear

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    private String email;

    @NotNull(message = "El rol es obligatorio")
    private Role role;

    private Long branchId;

    private String position;

    private String phone;

    private boolean active = true;

    public EmployeeDto() {
    }

    public EmployeeDto(Long id, String username, String password, String fullName, String email, Role role, Long branchId, String position, String phone, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.branchId = branchId;
        this.position = position;
        this.phone = phone;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
