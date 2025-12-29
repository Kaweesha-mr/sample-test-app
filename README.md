# E-Commerce Order Management System

A sample Spring Boot application demonstrating test prioritization with dependency graph analysis.

## Features

- **User Management**: Create, update, and manage users
- **Product Catalog**: Manage products and inventory
- **Order Processing**: Create and track orders
- **Payment Processing**: Handle payments and refunds
- **Email Notifications**: Send order confirmations and updates
- **Audit Logging**: Track system activities

## Architecture

The application follows a layered architecture:
- **Models**: JPA entities (User, Product, Order, Payment)
- **Repositories**: Data access layer
- **Services**: Business logic layer with dependencies between services
- **Controllers**: REST API endpoints
- **Tests**: Comprehensive unit tests for all components

## Dependencies Between Components

```
OrderService depends on:
  - UserService
  - ProductService
  - PaymentService
  - EmailService
  - AuditService

PaymentService depends on:
  - EmailService
  - AuditService

ProductService depends on:
  - AuditService

UserService depends on:
  - EmailService
  - AuditService
```

This creates a rich dependency graph perfect for testing the test prioritization system.

## Running Tests

```bash
mvn clean test
```

## Building

```bash
mvn clean package
```
