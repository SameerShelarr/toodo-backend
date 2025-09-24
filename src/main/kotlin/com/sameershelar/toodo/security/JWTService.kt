package com.sameershelar.toodo.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import java.util.Date

@Service
class JWTService(
    @param:Value("\${jwt.secret}") private val jwtSecret: String,
) {

    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    private val accessTokenValidity = 15 * 60 * 1000L // 15 minutes

    val refreshTokenValidity = 7 * 24 * 60 * 60 * 1000L // 7 days

    private fun generateToken(
        userId: String,
        type: String,
        expiry: Long,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)

        return Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun generateAccessToken(userId: String): String {
        return generateToken(
            userId = userId,
            type = "access",
            expiry = accessTokenValidity,
        )
    }

    fun generateRefreshToken(userId: String): String {
        return generateToken(
            userId = userId,
            type = "refresh",
            expiry = refreshTokenValidity,
        )
    }

    fun validateToken(token: String, type: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val isTypeValid = claims["type"] == type
        val now = Date()
        val isNotExpired = claims.expiration.after(now)

        return isTypeValid && isNotExpired
    }

    fun getUserIdFromToken(token: String): String {
        val claims =
            parseAllClaims(token) ?: throw ResponseStatusException(
                HttpStatusCode.valueOf(401),
                "Invalid token"
            )
        return claims.subject
    }

    private fun parseAllClaims(token: String): Claims? {
        val rawToken = token.removePrefix("Bearer ").trim()

        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch (_: Exception) {
            null
        }
    }
}