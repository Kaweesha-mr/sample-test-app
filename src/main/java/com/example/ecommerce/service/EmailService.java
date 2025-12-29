package com.example.ecommerce.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email Service - Handles all email notifications
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    public void sendWelcomeEmail(String email, String firstName) {
        logger.info("Sending welcome email to: {} ({})", email, firstName);
        // Simulate email sending
    }
    
    public void sendOrderConfirmation(String email, String orderNumber) {
        logger.info("Sending order confirmation email to: {} for order: {}", email, orderNumber);
        // Simulate email sending
    }
    
    public void sendPaymentConfirmation(String email, String orderNumber, String transactionId) {
        logger.info("Sending payment confirmation email to: {} for order: {} (transaction: {})", 
                   email, orderNumber, transactionId);
        // Simulate email sending
    }
    
    public void sendOrderShipped(String email, String orderNumber, String trackingNumber) {
        logger.info("Sending order shipped email to: {} for order: {} (tracking: {})", 
                   email, orderNumber, trackingNumber);
        // Simulate email sending
    }
    
    public void sendPasswordReset(String email, String resetToken) {
        logger.info("Sending password reset email to: {}", email);
        // Simulate email sending
    }
    
    public void sendLowStockAlert(String productName, int currentStock) {
        logger.warn("Low stock alert for product: {} (stock: {})", productName, currentStock);
        // Simulate email sending to admin
    }
}
