# toodo-backend

[![Main Build](https://github.com/SameerShelarr/toodo-backend/actions/workflows/main-build.yml/badge.svg?branch=main)](https://github.com/SameerShelarr/toodo-backend/actions/workflows/main-build.yml)
[![PR Tests](https://github.com/SameerShelarr/toodo-backend/actions/workflows/pr-tests.yml/badge.svg)](https://github.com/SameerShelarr/toodo-backend/actions/workflows/pr-tests.yml)

## Overview

toodo-backend is a Kotlin + Spring Boot REST API that powers the [**toodo**](https://github.com/SameerShelarr/toodo-multi-platform) productivity app (A Kotlin multiplatform app). It provides JWT-based authentication, MongoDB persistence, and endpoints for creating, retrieving, and managing todos tied to individual users.

### Features
- Registration and login with BCrypt-hashed passwords
- Access and refresh JWT token issuance with rotation on refresh
- Stateless authentication using a custom JWT filter
- MongoDB persistence for users, todos, and refresh tokens
- Validation and centralized exception handling for consistent API responses

## Tech Stack
- Kotlin 1.9
- Spring Boot 3.5 (Web, Security, Validation)
- MongoDB with Spring Data MongoDB (imperative)
- JJWT for token generation and verification
- Gradle Kotlin DSL build

## Getting Started

### Prerequisites
- JDK 17+
- MongoDB instance (local or hosted)
- Gradle (optional, you can use the provided `./gradlew` wrapper)

### Project Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/SameerShelarr/toodo-backend.git
   cd toodo-backend
   ```
2. Open the project in your IDE (IntelliJ IDEA recommended).
3. Run the `ToodoApplication` main class once using the gutter/run button so IntelliJ generates a run configuration (the first run may fail while variables are unset).
4. In **Run/Debug Configurations → Environment variables**, add:
   - `MONGODB_CONNECTION_STRING`: a full MongoDB connection URI (e.g. `mongodb://localhost:27017/toodo`).
   - `JWT_SECRET_BASE_64`: Base64-encoded string at least 256 bits when decoded (e.g. generate with `openssl rand -base64 32`).

### Running the Application
- From your IDE: run the `ToodoApplication` configuration after setting the environment variables.

The service starts on `http://localhost:8080` by default.

## API Reference

### Auth
- `POST /auth/register` – register with `email` and `password`
- `POST /auth/login` – obtain `accessToken` and `refreshToken`
- `POST /auth/refresh` – rotate refresh token and retrieve a new pair

Password requirements: at least 9 characters, containing uppercase, lowercase, and digits.

### Todos
- `POST /todos` – create or update a todo
- `GET /todos` – list todos for the authenticated user
- `DELETE /todos/{id}` – delete a todo owned by the user

Include the `Authorization: Bearer <accessToken>` header on secured endpoints.

## Testing
Run the unit tests:
```bash
./gradlew test
```

## Additional Notes
- Refresh tokens are hashed with SHA-256 before storage and expire after 7 days (MongoDB TTL index).
- Access tokens expire after 15 minutes; only refresh tokens can generate new access tokens.
- Validation errors return a 400 response with an `errors` array.

