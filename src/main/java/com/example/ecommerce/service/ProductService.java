package com.example.ecommerce.service;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AuditService auditService;
    
    private static final int LOW_STOCK_THRESHOLD = 10;
    
    public Product createProduct(String name, String description, BigDecimal price, Integer stockQuantity, String category) {
        Product product = new Product(name, description, price, stockQuantity, category);
        Product savedProduct = productRepository.save(product);
        
        // Log audit
        auditService.logProductCreated(savedProduct.getId(), name);
        
        return savedProduct;
    }
    
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public List<Product> getActiveProducts() {
        return productRepository.findByActive(true);
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public List<Product> getInStockProducts() {
        return productRepository.findByStockQuantityGreaterThan(0);
    }
    
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContaining(keyword);
    }
    
    public Product updateProduct(Long id, String name, String description, BigDecimal price, String category) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        
        if (!optionalProduct.isPresent()) {
            throw new IllegalArgumentException("Product not found with ID: " + id);
        }
        
        Product product = optionalProduct.get();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setUpdatedAt(LocalDateTime.now());
        
        Product updatedProduct = productRepository.save(product);
        
        // Log audit
        auditService.logProductUpdated(id, name);
        
        return updatedProduct;
    }
    
    public void reduceStock(Long productId, int quantity) {
        Optional<Product> optionalProduct = productRepository.findById(productId);
        
        if (!optionalProduct.isPresent()) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }
        
        Product product = optionalProduct.get();
        int oldStock = product.getStockQuantity();
        
        product.reduceStock(quantity);
        productRepository.save(product);
        
        // Log audit
        auditService.logStockUpdated(productId, oldStock, product.getStockQuantity());
        
        // Check for low stock and send alert
        if (product.getStockQuantity() <= LOW_STOCK_THRESHOLD) {
            emailService.sendLowStockAlert(product.getName(), product.getStockQuantity());
        }
    }
    
    public void increaseStock(Long productId, int quantity) {
        Optional<Product> optionalProduct = productRepository.findById(productId);
        
        if (!optionalProduct.isPresent()) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }
        
        Product product = optionalProduct.get();
        int oldStock = product.getStockQuantity();
        
        product.increaseStock(quantity);
        productRepository.save(product);
        
        // Log audit
        auditService.logStockUpdated(productId, oldStock, product.getStockQuantity());
    }
    
    public boolean isProductAvailable(Long productId, int requestedQuantity) {
        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (!optionalProduct.isPresent()) {
            return false;
        }
        
        Product product = optionalProduct.get();
        return product.isInStock() && product.getStockQuantity() >= requestedQuantity;
    }
}
