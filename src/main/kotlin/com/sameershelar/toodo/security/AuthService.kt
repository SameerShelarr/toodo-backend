package com.sameershelar.toodo.security

import com.sameershelar.toodo.db.model.RefreshToken
import com.sameershelar.toodo.db.model.User
import com.sameershelar.toodo.db.repository.RefreshTokenRepository
import com.sameershelar.toodo.db.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val jwtService: JWTService,
    private val passwordHashEncoder: PasswordHashEncoder,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
    )

    fun register(
        email: String,
        password: String,
    ): User {
        val user = userRepository.findByEmail(email)
        if (user != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists")
        }
        return userRepository.save(
            User(
                email = email,
                password = passwordHashEncoder.encode(password),
            )
        )
    }

    fun login(
        email: String,
        password: String,
    ): TokenPair {
        // Verify user exists
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("Invalid email or password")

        // Verify password
        if (passwordHashEncoder.matches(rawPassword = password, user.password).not()) {
            throw BadCredentialsException("Invalid email or password")
        }

        // Generate tokens
        val newAccessToken = jwtService.generateAccessToken(userId = user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(userId = user.id.toHexString())

        storeRefreshToken(
            userId = user.id,
            rawRefreshToken = newRefreshToken,
        )

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    @Transactional
    fun refresh(
        refreshToken: String,
    ): TokenPair {
        // Validate refresh token
        if (jwtService.validateToken(token = refreshToken, type = "refresh").not()) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401),"Invalid refresh token")
        }

        // Extract user ID from token
        val userId = jwtService.getUserIdFromToken(refreshToken)

        // Verify user exists
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            throw ResponseStatusException(HttpStatusCode.valueOf(401),"Invalid refresh token")
        }

        val token = hashToken(refreshToken)

        // Verify refresh token exists in DB
        refreshTokenRepository.findByUserIdAndToken(
            userId = user.id,
            token = token,
        ) ?: throw ResponseStatusException(HttpStatusCode.valueOf(401),"Invalid refresh token")

        // Invalidate the used refresh token
        refreshTokenRepository.deleteByUserIdAndToken(
            userId = user.id,
            token = token,
        )

        // Generate new tokens
        val newAccessToken = jwtService.generateAccessToken(userId = user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(userId = user.id.toHexString())

        // Store the new refresh token
        storeRefreshToken(
            userId = user.id,
            rawRefreshToken = newRefreshToken,
        )

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    private fun storeRefreshToken(
        userId: ObjectId,
        rawRefreshToken: String,
    ) {
        val token = hashToken(rawRefreshToken)
        val expiryMillisecond = jwtService.refreshTokenValidity
        val expiresAt = Instant.now().plusMillis(expiryMillisecond)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                token = token,
                expiresAt = expiresAt,
            )
        )
    }

    private fun hashToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawToken.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}