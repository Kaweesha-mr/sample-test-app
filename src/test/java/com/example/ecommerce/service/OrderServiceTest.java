package com.example.ecommerce.service;

import com.example.ecommerce.model.*;
import com.example.ecommerce.repository.OrderRepository;
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
 * Tests for OrderService - The most complex service
 * Dependencies: OrderRepository, UserService, ProductService, PaymentService, EmailService, AuditService
 * 
 * This demonstrates how changes in any dependency will affect this test
 */
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private ProductService productService;
    
    @Mock
    private PaymentService paymentService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private OrderService orderService;
    
    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private Payment testPayment;
    
    @BeforeEach
    public void setUp() {
        testUser = new User("John", "Doe", "john@example.com", "password123", "1234567890");
        testUser.setId(1L);
        
        testProduct = new Product("Test Product", "Description", new BigDecimal("99.99"), 100, "Electronics");
        testProduct.setId(1L);
        
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-123");
        testOrder.setUserId(1L);
        testOrder.setOrderStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(new BigDecimal("199.98"));
        
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(1L);
        testPayment.setAmount(new BigDecimal("199.98"));
        testPayment.setPaymentStatus(PaymentStatus.COMPLETED);
    }
    
    @Test
    public void testCreateOrder_Success() {
        OrderItem item1 = new OrderItem(1L, 2, new BigDecimal("99.99"));
        List<OrderItem> items = Arrays.asList(item1);
        
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
        when(productService.isProductAvailable(1L, 2)).thenReturn(true);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(productService).reduceStock(anyLong(), anyInt());
        when(paymentService.createPayment(anyLong(), any(BigDecimal.class), any(PaymentMethod.class)))
            .thenReturn(testPayment);
        doNothing().when(emailService).sendOrderConfirmation(anyString(), anyString());
        doNothing().when(auditService).logOrderCreated(anyLong(), anyString(), anyLong());
        
        Order result = orderService.createOrder(1L, items, PaymentMethod.CREDIT_CARD);
        
        assertNotNull(result);
        verify(userService, times(1)).getUserById(1L);
        verify(productService, times(1)).isProductAvailable(1L, 2);
        verify(productService, times(1)).reduceStock(1L, 2);
        verify(paymentService, times(1)).createPayment(anyLong(), any(BigDecimal.class), eq(PaymentMethod.CREDIT_CARD));
        verify(emailService, times(1)).sendOrderConfirmation("john@example.com", anyString());
        verify(auditService, times(1)).logOrderCreated(anyLong(), anyString(), eq(1L));
    }
    
    @Test
    public void testCreateOrder_UserNotFound() {
        OrderItem item1 = new OrderItem(1L, 2, new BigDecimal("99.99"));
        List<OrderItem> items = Arrays.asList(item1);
        
        when(userService.getUserById(anyLong())).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(999L, items, PaymentMethod.CREDIT_CARD);
        });
        
        verify(productService, never()).isProductAvailable(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
    }
    
    @Test
    public void testCreateOrder_ProductNotAvailable() {
        OrderItem item1 = new OrderItem(1L, 200, new BigDecimal("99.99"));
        List<OrderItem> items = Arrays.asList(item1);
        
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
        when(productService.isProductAvailable(1L, 200)).thenReturn(false);
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));
        
        assertThrows(IllegalStateException.class, () -> {
            orderService.createOrder(1L, items, PaymentMethod.CREDIT_CARD);
        });
        
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentService, never()).createPayment(anyLong(), any(BigDecimal.class), any(PaymentMethod.class));
    }
    
    @Test
    public void testGetOrderById_Found() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        
        Optional<Order> result = orderService.getOrderById(1L);
        
        assertTrue(result.isPresent());
        assertEquals(testOrder.getId(), result.get().getId());
    }
    
    @Test
    public void testGetOrderByOrderNumber_Found() {
        when(orderRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(testOrder));
        
        Optional<Order> result = orderService.getOrderByOrderNumber("ORD-123");
        
        assertTrue(result.isPresent());
        assertEquals(testOrder.getOrderNumber(), result.get().getOrderNumber());
    }
    
    @Test
    public void testConfirmOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(auditService).logOrderStatusChanged(anyLong(), anyString(), anyString(), anyString());
        
        Order result = orderService.confirmOrder(1L);
        
        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, result.getOrderStatus());
        verify(auditService, times(1)).logOrderStatusChanged(eq(1L), anyString(), eq("PENDING"), eq("CONFIRMED"));
    }
    
    @Test
    public void testProcessOrder_Success() {
        testOrder.setOrderStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(auditService).logOrderStatusChanged(anyLong(), anyString(), anyString(), anyString());
        
        Order result = orderService.processOrder(1L);
        
        assertNotNull(result);
        assertEquals(OrderStatus.PROCESSING, result.getOrderStatus());
        verify(auditService, times(1)).logOrderStatusChanged(eq(1L), anyString(), eq("CONFIRMED"), eq("PROCESSING"));
    }
    
    @Test
    public void testShipOrder_Success() {
        testOrder.setOrderStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(emailService).sendOrderShipped(anyString(), anyString(), anyString());
        doNothing().when(auditService).logOrderStatusChanged(anyLong(), anyString(), anyString(), anyString());
        
        Order result = orderService.shipOrder(1L, "TRACK-789");
        
        assertNotNull(result);
        assertEquals(OrderStatus.SHIPPED, result.getOrderStatus());
        assertEquals("TRACK-789", result.getTrackingNumber());
        verify(emailService, times(1)).sendOrderShipped("john@example.com", anyString(), "TRACK-789");
        verify(auditService, times(1)).logOrderStatusChanged(eq(1L), anyString(), eq("PROCESSING"), eq("SHIPPED"));
    }
    
    @Test
    public void testCompleteOrder_Success() {
        testOrder.setOrderStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(auditService).logOrderStatusChanged(anyLong(), anyString(), anyString(), anyString());
        
        Order result = orderService.completeOrder(1L);
        
        assertNotNull(result);
        assertEquals(OrderStatus.DELIVERED, result.getOrderStatus());
        verify(auditService, times(1)).logOrderStatusChanged(eq(1L), anyString(), eq("SHIPPED"), eq("DELIVERED"));
    }
    
    @Test
    public void testCancelOrder_Success() {
        testOrder.setPaymentId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentService.refundPayment(1L)).thenReturn(testPayment);
        doNothing().when(auditService).logOrderStatusChanged(anyLong(), anyString(), anyString(), anyString());
        
        Order result = orderService.cancelOrder(1L);
        
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getOrderStatus());
        verify(paymentService, times(1)).refundPayment(1L);
        verify(auditService, times(1)).logOrderStatusChanged(eq(1L), anyString(), eq("PENDING"), eq("CANCELLED"));
    }
    
    @Test
    public void testGetUserOrders() {
        when(orderRepository.findByUserId(1L)).thenReturn(Arrays.asList(testOrder));
        
        List<Order> result = orderService.getUserOrders(1L);
        
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findByUserId(1L);
    }
}
