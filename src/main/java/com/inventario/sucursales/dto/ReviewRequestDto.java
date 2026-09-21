package com.inventario.sucursales.dto;

// # DTO con observaciones del Administrador para aprobación o rechazo de solicitudes
public class ReviewRequestDto {

    private String adminComments;

    public ReviewRequestDto() {
    }

    public ReviewRequestDto(String adminComments) {
        this.adminComments = adminComments;
    }

    public String getAdminComments() {
        return adminComments;
    }

    public void setAdminComments(String adminComments) {
        this.adminComments = adminComments;
    }
}
