package com.example.restapi.model

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime

data class User(
  val id: String,
  val username: String,
  val email: String,
  val passwordHash: String,
  val createdAt: OffsetDateTime? = null,
  val roles: List<String> = emptyList(),
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("id", id)
      put("username", username)
      put("email", email)
      put("roles", JsonArray(roles))
      put("createdAt", createdAt?.toString())
    }
}
