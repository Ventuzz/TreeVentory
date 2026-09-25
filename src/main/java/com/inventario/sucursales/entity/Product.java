package com.inventario.sucursales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// Entidad JPA del catálogo de productos con precio, categoría y stock mínimo
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El SKU es obligatorio")
    @Column(nullable = false, unique = true, length = 30)
    private String sku;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @NotBlank(message = "La categoria es obligatoria")
    @Column(nullable = false, length = 50)
    private String category;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 20)
    private String unit = "Pza";

    @NotNull(message = "El umbral minimo de stock es obligatorio")
    @Min(value = 1, message = "El umbral minimo debe ser al menos 1")
    @Column(nullable = false)
    private Integer minStockThreshold = 10;

    @Column(nullable = false)
    private boolean active = true;

    public Product() {
    }

    public Product(Long id, String sku, String name, String description, String category, BigDecimal price, String unit,
            Integer minStockThreshold, boolean active) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.unit = unit;
        this.minStockThreshold = minStockThreshold;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getMinStockThreshold() {
        return minStockThreshold;
    }

    public void setMinStockThreshold(Integer minStockThreshold) {
        this.minStockThreshold = minStockThreshold;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
