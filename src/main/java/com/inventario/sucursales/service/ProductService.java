package com.inventario.sucursales.service;

import com.inventario.sucursales.entity.Product;
import com.inventario.sucursales.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional
    public Product createProduct(Product product) {
        if (productRepository.existsBySku(product.getSku())) {
            throw new IllegalArgumentException("Ya existe un producto con el SKU: " + product.getSku());
        }
        product.setActive(true);
        if (product.getMinStockThreshold() == null || product.getMinStockThreshold() < 1) {
            product.setMinStockThreshold(10);
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product updated) {
        Product product = getProductById(id);
        if (!product.getSku().equals(updated.getSku()) && productRepository.existsBySku(updated.getSku())) {
            throw new IllegalArgumentException("El nuevo SKU ya esta asignado a otro producto: " + updated.getSku());
        }
        product.setSku(updated.getSku());
        product.setName(updated.getName());
        product.setDescription(updated.getDescription());
        product.setCategory(updated.getCategory());
        product.setPrice(updated.getPrice());
        product.setUnit(updated.getUnit());
        product.setMinStockThreshold(updated.getMinStockThreshold());
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }
}
