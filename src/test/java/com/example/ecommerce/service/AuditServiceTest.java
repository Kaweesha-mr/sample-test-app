package com.example.ecommerce.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuditServiceTest {
    
    @InjectMocks
    private AuditService auditService;
    
    @Test
    public void testLogUserCreated() {
        assertDoesNotThrow(() -> {
            auditService.logUserCreated(1L, "test@example.com");
        });
    }
    
    @Test
    public void testLogUserUpdated() {
        assertDoesNotThrow(() -> {
            auditService.logUserUpdated(1L, "test@example.com");
        });
    }
    
    @Test
    public void testLogProductCreated() {
        assertDoesNotThrow(() -> {
            auditService.logProductCreated(1L, "Test Product");
        });
    }
    
    @Test
    public void testLogStockUpdated() {
        assertDoesNotThrow(() -> {
            auditService.logStockUpdated(1L, 100, 90);
        });
    }
    
    @Test
    public void testLogOrderCreated() {
        assertDoesNotThrow(() -> {
            auditService.logOrderCreated(1L, "ORD-123", 1L);
        });
    }
    
    @Test
    public void testLogPaymentProcessed() {
        assertDoesNotThrow(() -> {
            auditService.logPaymentProcessed(1L, "TXN-123", "COMPLETED");
        });
    }
}
