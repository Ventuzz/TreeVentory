package com.inventario.sucursales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// Entidad JPA para registro y trazabilidad de solicitudes de traspaso y surtido
@Entity
@Table(name = "inventory_requests")
public class InventoryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "El tipo de solicitud es obligatorio")
    @Column(name = "request_type", nullable = false, length = 20)
    private RequestType requestType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "origin_branch_id")
    private Branch originBranch; // Null if request is to external SUPPLIER

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_branch_id", nullable = false)
    @NotNull(message = "La sucursal destino es obligatoria")
    private Branch destinationBranch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "El producto es obligatorio")
    private Product product;

    @NotNull(message = "La cantidad solicitada es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "El estado de la solicitud es obligatorio")
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(length = 500)
    private String notes;

    @Column(name = "admin_comments", length = 500)
    private String adminComments;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public InventoryRequest() {
    }

    public InventoryRequest(Long id, RequestType requestType, Branch originBranch, Branch destinationBranch,
            Product product, Integer quantity, RequestStatus status, User requester, User reviewer, String notes,
            String adminComments, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.requestType = requestType;
        this.originBranch = originBranch;
        this.destinationBranch = destinationBranch;
        this.product = product;
        this.quantity = quantity;
        this.status = status;
        this.requester = requester;
        this.reviewer = reviewer;
        this.notes = notes;
        this.adminComments = adminComments;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public Branch getOriginBranch() {
        return originBranch;
    }

    public void setOriginBranch(Branch originBranch) {
        this.originBranch = originBranch;
    }

    public Branch getDestinationBranch() {
        return destinationBranch;
    }

    public void setDestinationBranch(Branch destinationBranch) {
        this.destinationBranch = destinationBranch;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAdminComments() {
        return adminComments;
    }

    public void setAdminComments(String adminComments) {
        this.adminComments = adminComments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
