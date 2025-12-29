package com.example.ecommerce.service;

import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for UserService
 * Dependencies: UserRepository, EmailService, AuditService
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    
    @BeforeEach
    public void setUp() {
        testUser = new User("John", "Doe", "john@example.com", "password123", "1234567890");
        testUser.setId(1L);
    }
    
    @Test
    public void testCreateUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());
        doNothing().when(auditService).logUserCreated(anyLong(), anyString());
        
        User result = userService.createUser("John", "Doe", "john@example.com", "password123", "1234567890");
        
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendWelcomeEmail("john@example.com", "John");
        verify(auditService, times(1)).logUserCreated(anyLong(), eq("john@example.com"));
    }
    
    @Test
    public void testCreateUser_EmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("John", "Doe", "john@example.com", "password123", "1234567890");
        });
        
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }
    
    @Test
    public void testGetUserById_Found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        Optional<User> result = userService.getUserById(1L);
        
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
    }
    
    @Test
    public void testGetUserByEmail_Found() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        
        Optional<User> result = userService.getUserByEmail("john@example.com");
        
        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
    }
    
    @Test
    public void testGetAllUsers() {
        User user2 = new User("Jane", "Smith", "jane@example.com", "pass456", "0987654321");
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));
        
        List<User> result = userService.getAllUsers();
        
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }
    
    @Test
    public void testUpdateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(auditService).logUserUpdated(anyLong(), anyString());
        
        User result = userService.updateUser(1L, "John Updated", "Doe", "1111111111", 
                                            "123 Main St", "New York", "10001");
        
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditService, times(1)).logUserUpdated(1L, "john@example.com");
    }
    
    @Test
    public void testUpdateUser_NotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(999L, "Test", "User", "1234567890", "Address", "City", "12345");
        });
    }
    
    @Test
    public void testDeactivateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(auditService).logUserUpdated(anyLong(), anyString());
        
        userService.deactivateUser(1L);
        
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditService, times(1)).logUserUpdated(1L, "john@example.com");
    }
}
