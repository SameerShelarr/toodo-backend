package com.sameershelar.toodo.controller

import com.sameershelar.toodo.db.model.Todo
import com.sameershelar.toodo.db.repository.TodoRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/todos")
class TodoController(
    private val repository: TodoRepository
) {

    data class TodoRequest(
        val id: String?,
        @field:NotBlank(message = "Title cannot be blank")
        val title: String,
        val isComplete: Boolean,
        val color: Long,
    )

    data class TodoResponse(
        val id: String,
        val title: String,
        val isComplete: Boolean,
        val color: Long,
        val createdAt: Instant,
    )

    // POST http://localhost:8080/todos
    @PostMapping
    fun save(
        @Valid @RequestBody body: TodoRequest,
    ): TodoResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val todo = repository.save(
            Todo(
                id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
                title = body.title,
                isComplete = body.isComplete,
                color = body.color,
                createdAt = Instant.now(),
                ownerId = ObjectId(ownerId),
            )
        )

        return todo.toResponse()
    }

    // GET http://localhost:8080/todos
    @GetMapping
    fun findAllByOwnerId(): List<TodoResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        return repository.findAllByOwnerId(ObjectId(ownerId)).map {
            it.toResponse()
        }
    }

    // DELETE http://localhost:8080/todos/64b64c3f5f3c2a6f4e8b4567
    @DeleteMapping(path = ["/{id}"])
    fun deleteById(
        @PathVariable id: String,
    ) {
        val todo = repository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Todo not found")
        }
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String

        if (todo.ownerId.toHexString() != ownerId) {
            throw IllegalAccessException("You are not authorized to delete this todo")
        }

        repository.deleteById(ObjectId(id))
    }
}

private fun Todo.toResponse() = TodoController.TodoResponse(
    id = this.id.toHexString(),
    title = this.title,
    isComplete = this.isComplete,
    color = this.color,
    createdAt = this.createdAt,
)