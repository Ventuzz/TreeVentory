package com.inventario.sucursales.dto;

// DTO de respuesta de autenticación con token JWT y datos del usuario
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private String username;
    private String fullName;
    private String role;
    private Long branchId;
    private String branchName;

    public LoginResponse() {
    }

    public LoginResponse(String token, String username, String fullName, String role, Long branchId,
            String branchName) {
        this.token = token;
        this.type = "Bearer";
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.branchId = branchId;
        this.branchName = branchName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
}
