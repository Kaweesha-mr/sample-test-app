package com.example.ecommerce.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Audit Service - Logs all important system activities
 */
@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    public void logUserCreated(Long userId, String email) {
        logger.info("[AUDIT] User created: ID={}, Email={}, Time={}", 
                   userId, email, LocalDateTime.now());
    }
    
    public void logUserUpdated(Long userId, String email) {
        logger.info("[AUDIT] User updated: ID={}, Email={}, Time={}", 
                   userId, email, LocalDateTime.now());
    }
    
    public void logProductCreated(Long productId, String productName) {
        logger.info("[AUDIT] Product created: ID={}, Name={}, Time={}", 
                   productId, productName, LocalDateTime.now());
    }
    
    public void logProductUpdated(Long productId, String productName) {
        logger.info("[AUDIT] Product updated: ID={}, Name={}, Time={}", 
                   productId, productName, LocalDateTime.now());
    }
    
    public void logStockUpdated(Long productId, int oldStock, int newStock) {
        logger.info("[AUDIT] Stock updated: ProductID={}, OldStock={}, NewStock={}, Time={}", 
                   productId, oldStock, newStock, LocalDateTime.now());
    }
    
    public void logOrderCreated(Long orderId, String orderNumber, Long userId) {
        logger.info("[AUDIT] Order created: ID={}, OrderNumber={}, UserID={}, Time={}", 
                   orderId, orderNumber, userId, LocalDateTime.now());
    }
    
    public void logOrderStatusChanged(Long orderId, String oldStatus, String newStatus) {
        logger.info("[AUDIT] Order status changed: ID={}, From={}, To={}, Time={}", 
                   orderId, oldStatus, newStatus, LocalDateTime.now());
    }
    
    public void logPaymentProcessed(Long paymentId, String transactionId, String status) {
        logger.info("[AUDIT] Payment processed: ID={}, TransactionID={}, Status={}, Time={}", 
                   paymentId, transactionId, status, LocalDateTime.now());
    }
    
    public void logPaymentRefunded(Long paymentId, String transactionId) {
        logger.info("[AUDIT] Payment refunded: ID={}, TransactionID={}, Time={}", 
                   paymentId, transactionId, LocalDateTime.now());
    }
}
