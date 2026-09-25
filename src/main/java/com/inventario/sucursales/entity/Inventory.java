package com.inventario.sucursales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// Entidad JPA que mapea las existencias actuales de un producto en una sucursal
@Entity
@Table(name = "inventories", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "branch_id", "product_id" })
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    @NotNull(message = "La sucursal es obligatoria")
    private Branch branch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "El producto es obligatorio")
    private Product product;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Inventory() {
    }

    public Inventory(Long id, Branch branch, Product product, Integer quantity, LocalDateTime lastUpdated) {
        this.id = id;
        this.branch = branch;
        this.product = product;
        this.quantity = quantity;
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isLowStock() {
        if (product == null || product.getMinStockThreshold() == null) {
            return false;
        }
        return this.quantity <= product.getMinStockThreshold();
    }
}
