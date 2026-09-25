package com.inventario.sucursales.dto;

import com.inventario.sucursales.entity.RequestType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// DTO con datos de entrada para creación de solicitudes de traspaso o surtido
public class CreateRequestDto {

    @NotNull(message = "El tipo de solicitud es obligatorio (TRANSFER o SUPPLIER)")
    private RequestType requestType;

    private Long originBranchId; // requerido solo para TRANSFER

    @NotNull(message = "La sucursal de destino es obligatoria")
    private Long destinationBranchId;

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser de al menos 1 unidad")
    private Integer quantity;

    private String notes;

    public CreateRequestDto() {
    }

    public CreateRequestDto(RequestType requestType, Long originBranchId, Long destinationBranchId, Long productId,
            Integer quantity, String notes) {
        this.requestType = requestType;
        this.originBranchId = originBranchId;
        this.destinationBranchId = destinationBranchId;
        this.productId = productId;
        this.quantity = quantity;
        this.notes = notes;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public Long getOriginBranchId() {
        return originBranchId;
    }

    public void setOriginBranchId(Long originBranchId) {
        this.originBranchId = originBranchId;
    }

    public Long getDestinationBranchId() {
        return destinationBranchId;
    }

    public void setDestinationBranchId(Long destinationBranchId) {
        this.destinationBranchId = destinationBranchId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
