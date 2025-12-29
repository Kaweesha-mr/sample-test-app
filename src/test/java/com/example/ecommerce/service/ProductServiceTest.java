package com.example.ecommerce.service;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ProductService
 * Dependencies: ProductRepository, EmailService, AuditService
 */
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private ProductService productService;
    
    private Product testProduct;
    
    @BeforeEach
    public void setUp() {
        testProduct = new Product("Test Product", "Description", new BigDecimal("99.99"), 100, "Electronics");
        testProduct.setId(1L);
    }
    
    @Test
    public void testCreateProduct_Success() {
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(auditService).logProductCreated(anyLong(), anyString());
        
        Product result = productService.createProduct("Test Product", "Description", new BigDecimal("99.99"), 100, "Electronics");
        
        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        verify(productRepository, times(1)).save(any(Product.class));
        verify(auditService, times(1)).logProductCreated(anyLong(), eq("Test Product"));
    }
    
    @Test
    public void testGetProductById_Found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        
        Optional<Product> result = productService.getProductById(1L);
        
        assertTrue(result.isPresent());
        assertEquals(testProduct.getId(), result.get().getId());
    }
    
    @Test
    public void testGetAllProducts() {
        Product product2 = new Product("Product 2", "Desc", new BigDecimal("49.99"), 50, "Books");
        when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct, product2));
        
        List<Product> result = productService.getAllProducts();
        
        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll();
    }
    
    @Test
    public void testUpdateProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(auditService).logProductUpdated(anyLong(), anyString());
        
        Product result = productService.updateProduct(1L, "Updated Product", "New Description", 
                                                      new BigDecimal("109.99"), "Electronics");
        
        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
        verify(auditService, times(1)).logProductUpdated(1L, "Updated Product");
    }
    
    @Test
    public void testUpdateProduct_NotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            productService.updateProduct(999L, "Test", "Desc", new BigDecimal("10.00"), "Cat");
        });
    }
    
    @Test
    public void testReduceStock_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(auditService).logStockUpdated(anyLong(), anyInt(), anyInt());
        
        productService.reduceStock(1L, 10);
        
        verify(productRepository, times(1)).save(any(Product.class));
        verify(auditService, times(1)).logStockUpdated(eq(1L), eq(100), anyInt());
    }
    
    @Test
    public void testReduceStock_InsufficientStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        
        assertThrows(IllegalStateException.class, () -> {
            productService.reduceStock(1L, 200);
        });
        
        verify(productRepository, never()).save(any(Product.class));
    }
    
    @Test
    public void testReduceStock_TriggersLowStockAlert() {
        testProduct.setStockQuantity(15);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(auditService).logStockUpdated(anyLong(), anyInt(), anyInt());
        doNothing().when(emailService).sendLowStockAlert(anyString(), anyInt());
        
        productService.reduceStock(1L, 10);
        
        verify(emailService, times(1)).sendLowStockAlert(eq("Test Product"), anyInt());
    }
    
    @Test
    public void testIncreaseStock_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(auditService).logStockUpdated(anyLong(), anyInt(), anyInt());
        
        productService.increaseStock(1L, 50);
        
        verify(productRepository, times(1)).save(any(Product.class));
        verify(auditService, times(1)).logStockUpdated(eq(1L), eq(100), anyInt());
    }
    
    @Test
    public void testIsProductAvailable_True() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        
        boolean result = productService.isProductAvailable(1L, 10);
        
        assertTrue(result);
    }
    
    @Test
    public void testIsProductAvailable_False() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        
        boolean result = productService.isProductAvailable(1L, 200);
        
        assertFalse(result);
    }
}
