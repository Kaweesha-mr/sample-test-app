package com.example.ecommerce.service;

import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Order.OrderStatus;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.User;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.Payment.PaymentMethod;
import com.example.ecommerce.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AuditService auditService;
    
    @Transactional
    public Order createOrder(Long userId, Map<Long, Integer> productQuantities, PaymentMethod paymentMethod) {
        // Validate user exists
        Optional<User> optionalUser = userService.getUserById(userId);
        if (!optionalUser.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = optionalUser.get();
        
        // Validate all products are available
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            if (!productService.isProductAvailable(productId, quantity)) {
                throw new IllegalStateException("Product not available: " + productId);
            }
            
            Optional<Product> product = productService.getProductById(productId);
            if (product.isPresent()) {
                BigDecimal itemTotal = product.get().getPrice().multiply(new BigDecimal(quantity));
                totalAmount = totalAmount.add(itemTotal);
            }
        }
        
        // Create order
        String orderNumber = generateOrderNumber();
        Order order = new Order(orderNumber, userId, totalAmount);
        order.setShippingAddress(user.getAddress());
        order.setBillingAddress(user.getAddress());
        
        Order savedOrder = orderRepository.save(order);
        
        // Reduce product stock
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            productService.reduceStock(entry.getKey(), entry.getValue());
        }
        
        // Create payment
        Payment payment = paymentService.createPayment(savedOrder.getId(), totalAmount, paymentMethod);
        
        // Send order confirmation email
        emailService.sendOrderConfirmation(user.getEmail(), orderNumber);
        
        // Log audit
        auditService.logOrderCreated(savedOrder.getId(), orderNumber, userId);
        
        return savedOrder;
    }
    
    public Order confirmOrder(Long orderId) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        
        if (!optionalOrder.isPresent()) {
            throw new IllegalArgumentException("Order not found with ID: " + orderId);
        }
        
        Order order = optionalOrder.get();
        OrderStatus oldStatus = order.getStatus();
        
        order.setStatus(OrderStatus.CONFIRMED);
        Order updatedOrder = orderRepository.save(order);
        
        // Log audit
        auditService.logOrderStatusChanged(orderId, oldStatus.toString(), OrderStatus.CONFIRMED.toString());
        
        return updatedOrder;
    }
    
    public Order processOrder(Long orderId, String userEmail) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        
        if (!optionalOrder.isPresent()) {
            throw new IllegalArgumentException("Order not found with ID: " + orderId);
        }
        
        Order order = optionalOrder.get();
        
        // Process payment
        List<Payment> payments = paymentService.getPaymentsByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new IllegalStateException("No payment found for order: " + orderId);
        }
        
        Payment payment = payments.get(0);
        paymentService.processPayment(payment.getId(), userEmail, order.getOrderNumber());
        
        // Update order status
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.PROCESSING);
        Order updatedOrder = orderRepository.save(order);
        
        // Log audit
        auditService.logOrderStatusChanged(orderId, oldStatus.toString(), OrderStatus.PROCESSING.toString());
        
        return updatedOrder;
    }
    
    public Order shipOrder(Long orderId, String trackingNumber) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        
        if (!optionalOrder.isPresent()) {
            throw new IllegalArgumentException("Order not found with ID: " + orderId);
        }
        
        Order order = optionalOrder.get();
        OrderStatus oldStatus = order.getStatus();
        
        order.setStatus(OrderStatus.SHIPPED);
        Order updatedOrder = orderRepository.save(order);
        
        // Get user and send shipping notification
        Optional<User> optionalUser = userService.getUserById(order.getUserId());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            emailService.sendOrderShipped(user.getEmail(), order.getOrderNumber(), trackingNumber);
        }
        
        // Log audit
        auditService.logOrderStatusChanged(orderId, oldStatus.toString(), OrderStatus.SHIPPED.toString());
        
        return updatedOrder;
    }
    
    public Order completeOrder(Long orderId) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        
        if (!optionalOrder.isPresent()) {
            throw new IllegalArgumentException("Order not found with ID: " + orderId);
        }
        
        Order order = optionalOrder.get();
        OrderStatus oldStatus = order.getStatus();
        
        order.setStatus(OrderStatus.DELIVERED);
        order.setCompletedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        
        // Log audit
        auditService.logOrderStatusChanged(orderId, oldStatus.toString(), OrderStatus.DELIVERED.toString());
        
        return updatedOrder;
    }
    
    @Transactional
    public Order cancelOrder(Long orderId) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        
        if (!optionalOrder.isPresent()) {
            throw new IllegalArgumentException("Order not found with ID: " + orderId);
        }
        
        Order order = optionalOrder.get();
        OrderStatus oldStatus = order.getStatus();
        
        // Refund payment if order was already paid
        List<Payment> payments = paymentService.getPaymentsByOrderId(orderId);
        for (Payment payment : payments) {
            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                paymentService.refundPayment(payment.getId());
            }
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);
        
        // Log audit
        auditService.logOrderStatusChanged(orderId, oldStatus.toString(), OrderStatus.CANCELLED.toString());
        
        return updatedOrder;
    }
    
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }
    
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }
    
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
    public List<Order> getUserOrdersByStatus(Long userId, OrderStatus status) {
        return orderRepository.findByUserIdAndStatus(userId, status);
    }
    
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
