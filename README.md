# Brew Buddy Auth Service 🍺🔐

Modern authentication and user management service built with Spring Boot 3.4 and JWT tokens. Part of the Brew Buddy application ecosystem.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Security](#-security)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

## ✨ Features

### Authentication
- ✅ JWT-based authentication (access + refresh tokens)
- ✅ User registration with validation
- ✅ Secure login with BCrypt password hashing
- ✅ Token refresh mechanism
- ✅ Client-side stateless logout

### User Management
- ✅ Role-based access control (USER, ADMIN)
- ✅ User CRUD operations
- ✅ Profile management
- ✅ Password management
- ✅ Email verification status

### Security
- ✅ BCrypt password hashing (strength 12)
- ✅ JWT token signing with HMAC-SHA256
- ✅ Stateless session management
- ✅ CORS configuration
- ✅ Global exception handling

### Administration
- ✅ Admin dashboard
- ✅ User creation and management
- ✅ Role assignment
- ✅ Password reset

## 🛠 Tech Stack

### Backend
- **Java 21** - Latest LTS version
- **Spring Boot 3.4** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database abstraction
- **JJWT 0.12.5** - JWT token handling

### Database
- **PostgreSQL** - Production database (Supabase)
- **H2** - In-memory database for testing

### Build & Test
- **Gradle 8.5** - Build tool
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### Deployment
- **Docker** - Containerization
- **Fly.io** - Cloud deployment platform

## 🏗 Architecture

### Authentication Flow

```
┌─────────┐          ┌──────────────┐          ┌──────────┐
│ Client  │          │   Auth API   │          │ Database │
└────┬────┘          └──────┬───────┘          └────┬─────┘
     │                      │                       │
     │ 1. POST /register    │                       │
     │─────────────────────>│                       │
     │                      │ 2. Hash password      │
     │                      │ 3. Save user          │
     │                      │──────────────────────>│
     │                      │ 4. Generate tokens    │
     │ 5. Return tokens     │                       │
     │<─────────────────────│                       │
     │                      │                       │
     │ 6. POST /login       │                       │
     │─────────────────────>│                       │
     │                      │ 7. Validate password  │
     │                      │──────────────────────>│
     │                      │ 8. Generate tokens    │
     │ 9. Return tokens     │                       │
     │<─────────────────────│                       │
     │                      │                       │
     │ 10. GET /users/me    │                       │
     │ + Bearer token       │                       │
     │─────────────────────>│                       │
     │                      │ 11. Validate token    │
     │                      │ 12. Load user         │
     │                      │──────────────────────>│
     │ 13. Return user data │                       │
     │<─────────────────────│                       │
```

### Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Controllers                           │
│  AuthController  │  AdminController  │  UserProfileController│
└─────────────┬───────────────┬───────────────┬──────────────┘
              │               │               │
              ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────┐
│                         Services                             │
│  AuthenticationService  │  UserService  │  JwtService       │
└─────────────┬───────────────┬───────────────┬──────────────┘
              │               │               │
              ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Repositories                            │
│      UserRepository         │        UserRoleRepository      │
└─────────────┬───────────────┴───────────────┬──────────────┘
              │                               │
              ▼                               ▼
┌─────────────────────────────────────────────────────────────┐
│                         Database                             │
│              PostgreSQL / H2 (testing)                       │
└─────────────────────────────────────────────────────────────┘
```

### Security Filter Chain

```
HTTP Request
     │
     ▼
┌─────────────────────┐
│   CORS Filter       │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ JWT Auth Filter     │ ◄── Extract & validate JWT token
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Security Context   │ ◄── Set authentication
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│   Controller        │
└─────────────────────┘
```

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher
- Gradle 8.5 or higher (or use wrapper)
- PostgreSQL 16+ (or use Docker)
- Docker (optional, for containerization)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/brew-buddy-auth.git
   cd brew-buddy-auth
   ```

2. **Configure environment variables**


3. **Set up database** (if using local PostgreSQL)
   ```bash
   createdb brew-buddy
   ```

4. **Generate JWT secret**
   ```bash
   openssl rand -hex 32
   # Copy output to JWT_SECRET in .env
   ```

5. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

   The application will start on `http://localhost:8081`

### Quick Start with Docker

```bash
# Build and run with Docker Compose
docker-compose up --build

# Application available at http://localhost:8081
```

### Verify Installation

```bash
# Health check
curl http://localhost:8081/actuator/health

# Should return: {"status":"UP"}
```

## ⚙️ Configuration

### Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/brew-buddy
DB_USERNAME=postgres
DB_PASSWORD=your-password

# JWT Configuration (REQUIRED)
JWT_SECRET=your-secure-256-bit-secret-minimum-32-characters
JWT_EXPIRATION_MS=86400000          # 24 hours
JWT_REFRESH_EXPIRATION_MS=604800000 # 7 days
JWT_ISSUER=brew-buddy-auth

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Server Configuration (optional)
PORT=8081
```

### application.properties

Located at `src/main/resources/application.properties`:

## 📚 API Documentation

### Quick Reference

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/register` | POST | Public | Register new user |
| `/api/auth/login` | POST | Public | Login user |
| `/api/auth/refresh` | POST | Public | Refresh access token |
| `/api/auth/validate` | GET | Required | Validate token |
| `/api/auth/logout` | POST | Optional | Logout user |
| `/api/users/me` | GET | Required | Get current user |
| `/api/users/me` | PUT | Required | Update profile |
| `/api/admin/users` | POST | Admin | Create user |
| `/api/admin/users` | GET | Admin | List users |
| `/api/admin/users/{id}` | GET | Admin | Get user by ID |
| `/api/admin/users/{id}` | PUT | Admin | Update user |
| `/api/admin/users/{id}` | DELETE | Admin | Delete user |

### Example: Register & Login

```bash
# Register new user
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe"
  }'

# Response includes access and refresh tokens
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"]
  }
}

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePassword123!"
  }'

# Access protected endpoint
curl -X GET http://localhost:8081/api/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Complete API Documentation

Interactive API documentation is available via Swagger UI:

**Local Development:**
- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI Spec (JSON): http://localhost:8081/api-docs
- OpenAPI Spec (YAML): See [`openapi.yaml`](openapi.yaml)

**Features:**
- Try out endpoints directly in the browser
- View detailed request/response schemas
- See authentication requirements
- Copy curl commands automatically

## 🧪 Testing

### Run Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.brewbuddy.auth.service.JwtServiceTest"

# Run with coverage
./gradlew test jacocoTestReport

# Run integration tests only
./gradlew test --tests "com.brewbuddy.auth.integration.*"
```

## 🗺️ Roadmap

- [ ] Rate limiting for authentication endpoints
- [ ] Email verification flow
- [ ] Password reset via email
- [ ] Account lockout after failed attempts
- [ ] Audit logging
- [ ] API versioning
