package com.sameershelar.toodo.db.model

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("refresh_tokens")
data class RefreshToken(
    val userId: ObjectId,
    val token: String,
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
)
