package com.example.ecommerce.service;

import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Service - Manages user operations
 * Dependencies: UserRepository, EmailService, AuditService
 */
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AuditService auditService;
    
    public User createUser(String firstName, String lastName, String email, String password, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        User user = new User(firstName, lastName, email, password, phone);
        User savedUser = userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(email, firstName);
        
        // Log audit
        auditService.logUserCreated(savedUser.getId(), email);
        
        return savedUser;
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> getActiveUsers() {
        return userRepository.findByActive(true);
    }
    
    public User updateUser(Long id, String firstName, String lastName, String phone, String address, String city, String zipCode) {
        Optional<User> optionalUser = userRepository.findById(id);
        
        if (!optionalUser.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        
        User user = optionalUser.get();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setAddress(address);
        user.setCity(city);
        user.setZipCode(zipCode);
        
        User updatedUser = userRepository.save(user);
        
        // Log audit
        auditService.logUserUpdated(user.getId(), user.getEmail());
        
        return updatedUser;
    }
    
    public void updateLastLogin(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    public void deactivateUser(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setActive(false);
            userRepository.save(user);
            
            auditService.logUserUpdated(id, user.getEmail());
        }
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
