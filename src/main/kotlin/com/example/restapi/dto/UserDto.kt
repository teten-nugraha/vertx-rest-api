package com.example.restapi.dto

import com.example.restapi.model.User
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime

data class UserDto(
  val id: String,
  val username: String,
  val email: String,
  val roles: List<String>,
  val createdAt: OffsetDateTime? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("id", id)
      put("username", username)
      put("email", email)
      put("roles", JsonArray(roles))
      put("createdAt", createdAt?.toString())
    }

  companion object {
    fun fromUser(user: User): UserDto =
      UserDto(
        id = user.id,
        username = user.username,
        email = user.email,
        roles = user.roles,
        createdAt = user.createdAt,
      )
  }
}
