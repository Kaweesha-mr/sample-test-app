package com.example.ecommerce.service;

import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.PaymentMethod;
import com.example.ecommerce.model.PaymentStatus;
import com.example.ecommerce.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PaymentService
 * Dependencies: PaymentRepository, EmailService, AuditService
 */
@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private PaymentService paymentService;
    
    private Payment testPayment;
    
    @BeforeEach
    public void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(1L);
        testPayment.setAmount(new BigDecimal("199.99"));
        testPayment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        testPayment.setPaymentStatus(PaymentStatus.PENDING);
    }
    
    @Test
    public void testCreatePayment_Success() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        
        Payment result = paymentService.createPayment(1L, new BigDecimal("199.99"), PaymentMethod.CREDIT_CARD);
        
        assertNotNull(result);
        assertEquals(new BigDecimal("199.99"), result.getAmount());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        assertNotNull(result.getTransactionId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
    
    @Test
    public void testProcessPayment_Success() {
        testPayment.setTransactionId("TXN-123");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(emailService).sendPaymentConfirmation(anyString(), anyString(), anyString());
        doNothing().when(auditService).logPaymentProcessed(anyLong(), anyString(), anyString());
        
        Payment result = paymentService.processPayment(1L, "customer@example.com", "ORD-123");
        
        assertNotNull(result);
        verify(paymentRepository, atLeast(1)).save(any(Payment.class));
        verify(auditService, times(1)).logPaymentProcessed(eq(1L), eq("TXN-123"), anyString());
    }
    
    @Test
    public void testProcessPayment_NotFound() {
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(999L, "customer@example.com", "ORD-123");
        });
        
        verify(emailService, never()).sendPaymentConfirmation(anyString(), anyString(), anyString());
    }
    
    @Test
    public void testRefundPayment_Success() {
        testPayment.setPaymentStatus(PaymentStatus.COMPLETED);
        testPayment.setTransactionId("TXN-123");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(auditService).logPaymentRefunded(anyLong(), anyString());
        
        Payment result = paymentService.refundPayment(1L);
        
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, result.getPaymentStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(auditService, times(1)).logPaymentRefunded(1L, "TXN-123");
    }
    
    @Test
    public void testRefundPayment_NotCompleted() {
        testPayment.setPaymentStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        
        assertThrows(IllegalStateException.class, () -> {
            paymentService.refundPayment(1L);
        });
        
        verify(paymentRepository, never()).save(any(Payment.class));
    }
    
    @Test
    public void testGetPaymentById_Found() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        
        Optional<Payment> result = paymentService.getPaymentById(1L);
        
        assertTrue(result.isPresent());
        assertEquals(testPayment.getId(), result.get().getId());
    }
    
    @Test
    public void testGenerateTransactionId() {
        // This is a private method, but we can test it indirectly through createPayment
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        
        Payment result = paymentService.createPayment(1L, new BigDecimal("99.99"), PaymentMethod.PAYPAL);
        
        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("TXN-"));
    }
}
