package com.example.ecommerce.service;

import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.Payment.PaymentMethod;
import com.example.ecommerce.model.Payment.PaymentStatus;
import com.example.ecommerce.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AuditService auditService;
    
    public Payment createPayment(Long orderId, BigDecimal amount, PaymentMethod paymentMethod) {
        String transactionId = generateTransactionId();
        
        Payment payment = new Payment(orderId, transactionId, amount, paymentMethod);
        Payment savedPayment = paymentRepository.save(payment);
        
        // Log audit
        auditService.logPaymentProcessed(savedPayment.getId(), transactionId, "CREATED");
        
        return savedPayment;
    }
    
    public Payment processPayment(Long paymentId, String userEmail, String orderNumber) {
        Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);
        
        if (!optionalPayment.isPresent()) {
            throw new IllegalArgumentException("Payment not found with ID: " + paymentId);
        }
        
        Payment payment = optionalPayment.get();
        
        // Simulate payment processing
        boolean paymentSuccessful = simulatePaymentGateway(payment);
        
        if (paymentSuccessful) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());
            payment.setPaymentGatewayResponse("SUCCESS");
            
            // Send payment confirmation email
            emailService.sendPaymentConfirmation(userEmail, orderNumber, payment.getTransactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentGatewayResponse("DECLINED");
        }
        
        Payment updatedPayment = paymentRepository.save(payment);
        
        // Log audit
        auditService.logPaymentProcessed(paymentId, payment.getTransactionId(), payment.getStatus().toString());
        
        return updatedPayment;
    }
    
    public Payment refundPayment(Long paymentId) {
        Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);
        
        if (!optionalPayment.isPresent()) {
            throw new IllegalArgumentException("Payment not found with ID: " + paymentId);
        }
        
        Payment payment = optionalPayment.get();
        
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Can only refund completed payments");
        }
        
        payment.setStatus(PaymentStatus.REFUNDED);
        Payment refundedPayment = paymentRepository.save(payment);
        
        // Log audit
        auditService.logPaymentRefunded(paymentId, payment.getTransactionId());
        
        return refundedPayment;
    }
    
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }
    
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }
    
    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
    
    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING);
    }
    
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private boolean simulatePaymentGateway(Payment payment) {
        // Simulate payment gateway processing
        // In reality, this would call actual payment gateway API
        return payment.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}
