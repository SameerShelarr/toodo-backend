package com.sameershelar.toodo.db.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("todos")
data class Todo(
    val title: String,
    val isComplete: Boolean,
    val color: Long,
    val createdAt: Instant,
    val ownerId: ObjectId,
    @Id val id: ObjectId = ObjectId.get(),
)
