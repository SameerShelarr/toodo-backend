package com.sameershelar.toodo.db.repository

import com.sameershelar.toodo.db.model.Todo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface TodoRepository : MongoRepository<Todo, ObjectId> {
    fun findAllByOwnerId(ownerId: ObjectId): List<Todo>
}