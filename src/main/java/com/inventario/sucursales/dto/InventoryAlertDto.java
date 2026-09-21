package com.inventario.sucursales.dto;

public class InventoryAlertDto {

    private Long inventoryId;
    private Long branchId;
    private String branchName;
    private Long productId;
    private String productName;
    private String productSku;
    private String category;
    private Integer currentStock;
    private Integer minStockThreshold;
    private String alertLevel; // "CRITICAL" (0) or "WARNING" (<= minStockThreshold)
    private String alertMessage;

    public InventoryAlertDto() {
    }

    public InventoryAlertDto(Long inventoryId, Long branchId, String branchName, Long productId, String productName, String productSku, String category, Integer currentStock, Integer minStockThreshold) {
        this.inventoryId = inventoryId;
        this.branchId = branchId;
        this.branchName = branchName;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.category = category;
        this.currentStock = currentStock;
        this.minStockThreshold = minStockThreshold;
        if (currentStock <= 0) {
            this.alertLevel = "CRITICAL";
            this.alertMessage = "¡Sin existencias! Urge solicitar mercancía inmediatamente.";
        } else {
            this.alertLevel = "WARNING";
            this.alertMessage = "Stock bajo (" + currentStock + " de " + minStockThreshold + " mínimas). Considere solicitar reabastecimiento.";
        }
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getMinStockThreshold() {
        return minStockThreshold;
    }

    public void setMinStockThreshold(Integer minStockThreshold) {
        this.minStockThreshold = minStockThreshold;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }
}
