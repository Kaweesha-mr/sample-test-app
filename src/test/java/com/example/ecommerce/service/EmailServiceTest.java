package com.example.ecommerce.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {
    
    @InjectMocks
    private EmailService emailService;
    
    @Test
    public void testSendWelcomeEmail() {
        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail("test@example.com", "John");
        });
    }
    
    @Test
    public void testSendOrderConfirmation() {
        assertDoesNotThrow(() -> {
            emailService.sendOrderConfirmation("test@example.com", "ORD-123");
        });
    }
    
    @Test
    public void testSendPaymentConfirmation() {
        assertDoesNotThrow(() -> {
            emailService.sendPaymentConfirmation("test@example.com", "ORD-123", "TXN-456");
        });
    }
    
    @Test
    public void testSendOrderShipped() {
        assertDoesNotThrow(() -> {
            emailService.sendOrderShipped("test@example.com", "ORD-123", "TRACK-789");
        });
    }
    
    @Test
    public void testSendPasswordReset() {
        assertDoesNotThrow(() -> {
            emailService.sendPasswordReset("test@example.com", "RESET-TOKEN");
        });
    }
    
    @Test
    public void testSendLowStockAlert() {
        assertDoesNotThrow(() -> {
            emailService.sendLowStockAlert("Test Product", 5);
        });
    }
}
