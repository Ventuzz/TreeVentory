package com.inventario.sucursales.repository;

import com.inventario.sucursales.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// # Repositorio JPA para persistencia y filtrado del catálogo de productos
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    List<Product> findByCategory(String category);
    List<Product> findByActiveTrueOrderByNameAsc();
    boolean existsBySku(String sku);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true")
    List<String> findDistinctCategories();
}
