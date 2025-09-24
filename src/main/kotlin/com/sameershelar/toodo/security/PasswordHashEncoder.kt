package com.sameershelar.toodo.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordHashEncoder {

    private val bCrypt = BCryptPasswordEncoder()

    fun encode(rawPassword: String): String {
        return bCrypt.encode(rawPassword)
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return bCrypt.matches(rawPassword, encodedPassword)
    }
}