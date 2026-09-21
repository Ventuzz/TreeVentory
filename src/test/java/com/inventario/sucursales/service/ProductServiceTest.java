package com.inventario.sucursales.service;

import com.inventario.sucursales.entity.Product;
import com.inventario.sucursales.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product1;

    @BeforeEach
    void setUp() {
        product1 = new Product(1L, "PROD-001", "Laptop Pro", "Desc", "Electrónica", new BigDecimal("15000.00"), "Pza", 5, true);
    }

    @Test
    void testGetAllProducts() {
        when(productRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(product1));

        List<Product> list = productService.getAllProducts();

        assertEquals(1, list.size());
        assertEquals("PROD-001", list.get(0).getSku());
    }

    @Test
    void testGetProductByIdFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        Product found = productService.getProductById(1L);

        assertNotNull(found);
        assertEquals("Laptop Pro", found.getName());
    }

    @Test
    void testGetProductByIdNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> productService.getProductById(99L));
    }

    @Test
    void testCreateProductSuccess() {
        Product newProd = new Product(null, "PROD-002", "Mouse", "Desc", "Electrónica", new BigDecimal("300.00"), "Pza", 10, true);
        when(productRepository.existsBySku("PROD-002")).thenReturn(false);
        when(productRepository.save(newProd)).thenReturn(newProd);

        Product created = productService.createProduct(newProd);

        assertNotNull(created);
        assertTrue(created.isActive());
        verify(productRepository).save(newProd);
    }

    @Test
    void testCreateProductDuplicateSku() {
        Product dupProd = new Product(null, "PROD-001", "Otro", "Desc", "Electrónica", new BigDecimal("200.00"), "Pza", 10, true);
        when(productRepository.existsBySku("PROD-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dupProd));
    }

    @Test
    void testUpdateProduct() {
        Product updatedData = new Product(null, "PROD-001", "Laptop Pro Max", "Desc Editada", "Electrónica", new BigDecimal("18000.00"), "Pza", 8, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        Product result = productService.updateProduct(1L, updatedData);

        assertEquals("Laptop Pro Max", result.getName());
        assertEquals(8, result.getMinStockThreshold());
    }

    @Test
    void testDeleteProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        productService.deleteProduct(1L);

        assertFalse(product1.isActive());
        verify(productRepository).save(product1);
    }

    @Test
    void testGetCategories() {
        when(productRepository.findDistinctCategories()).thenReturn(List.of("Electrónica", "Alimentos"));

        List<String> categories = productService.getAllCategories();

        assertEquals(2, categories.size());
    }

    @Test
    void testGetProductsByCategory() {
        when(productRepository.findByCategory("Electrónica")).thenReturn(List.of(product1));

        List<Product> products = productService.getProductsByCategory("Electrónica");

        assertEquals(1, products.size());
    }
}
