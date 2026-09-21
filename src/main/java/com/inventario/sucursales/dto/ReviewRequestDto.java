package com.inventario.sucursales.dto;

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
