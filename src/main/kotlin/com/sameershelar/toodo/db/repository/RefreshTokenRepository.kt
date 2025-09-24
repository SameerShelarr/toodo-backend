package com.sameershelar.toodo.db.repository

import com.sameershelar.toodo.db.model.RefreshToken
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface RefreshTokenRepository : MongoRepository<RefreshToken, ObjectId> {
    fun findByUserIdAndToken(userId: ObjectId, token: String): RefreshToken?
    fun deleteByUserIdAndToken(userId: ObjectId, token: String)
}