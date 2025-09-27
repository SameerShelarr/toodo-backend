package com.sameershelar.toodo.security

import com.sameershelar.toodo.db.model.RefreshToken
import com.sameershelar.toodo.db.model.User
import com.sameershelar.toodo.db.repository.RefreshTokenRepository
import com.sameershelar.toodo.db.repository.UserRepository
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthServiceTest {

    @MockK
    lateinit var jwtService: JWTService

    @MockK
    lateinit var passwordHashEncoder: PasswordHashEncoder

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var refreshTokenRepository: RefreshTokenRepository

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        authService = AuthService(
            jwtService = jwtService,
            passwordHashEncoder = passwordHashEncoder,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
        )
    }

    @Test
    fun `register saves new user when email not taken`() {
        val email = "test@example.com"
        val password = "Password123"
        val hashedPassword = "hashedPwd"
        val savedUser = User(email = email, password = hashedPassword)

        every { userRepository.findByEmail(email) } returns null
        every { passwordHashEncoder.encode(password) } returns hashedPassword
        every { userRepository.save(any()) } returns savedUser

        val result = authService.register(email, password)

        assertEquals(savedUser, result)
        verify(exactly = 1) { passwordHashEncoder.encode(password) }
        verify {
            userRepository.save(match {
                it.email == email && it.password == hashedPassword
            })
        }
    }

    @Test
    fun `register throws conflict when user exists`() {
        val email = "duplicate@example.com"
        val password = "Password123"
        val existingUser = User(email = email, password = "existing")

        every { userRepository.findByEmail(email) } returns existingUser

        val exception = assertThrows<ResponseStatusException> {
            authService.register(email, password)
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login returns tokens and stores hashed refresh token`() {
        val email = "user@example.com"
        val password = "Password123"
        val storedPassword = "storedHash"
        val userId = ObjectId.get()
        val user = User(id = userId, email = email, password = storedPassword)
        val accessToken = "access-token"
        val refreshToken = "refresh-token"
        val refreshTokenSlot: CapturingSlot<RefreshToken> = slot()
        val expectedHashed = hashToken(refreshToken)

        every { userRepository.findByEmail(email) } returns user
        every { passwordHashEncoder.matches(password, storedPassword) } returns true
        every { jwtService.generateAccessToken(userId.toHexString()) } returns accessToken
        every { jwtService.generateRefreshToken(userId.toHexString()) } returns refreshToken
        every { jwtService.refreshTokenValidity } returns 1000L
        every { refreshTokenRepository.save(capture(refreshTokenSlot)) } answers { refreshTokenSlot.captured }

        val tokenPair = authService.login(email, password)

        assertEquals(accessToken, tokenPair.accessToken)
        assertEquals(refreshToken, tokenPair.refreshToken)

        val savedToken = refreshTokenSlot.captured
        assertEquals(userId, savedToken.userId)
        assertEquals(expectedHashed, savedToken.token)
        val approxExpiry = Duration.between(Instant.now(), savedToken.expiresAt).toMillis()
        assertTrue(approxExpiry in 0..1500)

        verify(exactly = 1) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `refresh validates token rotates and stores new refresh token`() {
        val userId = ObjectId.get()
        val refreshToken = "incoming-refresh"
        val hashedExisting = hashToken(refreshToken)
        val user = User(id = userId, email = "refresh@example.com", password = "stored")
        val newAccessToken = "new-access"
        val newRefreshToken = "new-refresh"
        val newHashed = hashToken(newRefreshToken)
        val savedNewTokenSlot: CapturingSlot<RefreshToken> = slot()

        every { jwtService.validateToken(refreshToken, "refresh") } returns true
        every { jwtService.getUserIdFromToken(refreshToken) } returns userId.toHexString()
        every { userRepository.findById(userId) } returns Optional.of(user)
        every {
            refreshTokenRepository.findByUserIdAndToken(userId, hashedExisting)
        } returns RefreshToken(userId = userId, token = hashedExisting, expiresAt = Instant.now().plusSeconds(10))
        every { refreshTokenRepository.deleteByUserIdAndToken(userId, hashedExisting) } just Runs
        every { jwtService.generateAccessToken(userId.toHexString()) } returns newAccessToken
        every { jwtService.generateRefreshToken(userId.toHexString()) } returns newRefreshToken
        every { jwtService.refreshTokenValidity } returns 1000L
        every { refreshTokenRepository.save(capture(savedNewTokenSlot)) } answers { savedNewTokenSlot.captured }

        val tokenPair = authService.refresh(refreshToken)

        assertEquals(newAccessToken, tokenPair.accessToken)
        assertEquals(newRefreshToken, tokenPair.refreshToken)

        verify { refreshTokenRepository.deleteByUserIdAndToken(userId, hashedExisting) }
        val savedNewToken = savedNewTokenSlot.captured
        assertEquals(userId, savedNewToken.userId)
        assertEquals(newHashed, savedNewToken.token)
    }

    private fun hashToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawToken.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}

