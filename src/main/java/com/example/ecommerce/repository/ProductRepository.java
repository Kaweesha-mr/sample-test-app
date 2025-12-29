package com.example.ecommerce.repository;

import com.example.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByCategory(String category);
    
    List<Product> findByActive(Boolean active);
    
    List<Product> findByStockQuantityGreaterThan(Integer quantity);
    
    List<Product> findByNameContaining(String name);
}
