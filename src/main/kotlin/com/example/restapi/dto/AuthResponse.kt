package com.example.restapi.dto

import io.vertx.core.json.JsonObject

data class AuthResponse(
  val accessToken: String,
  val refreshToken: String,
  val tokenType: String = "Bearer",
  val expiresIn: Long,
  val user: UserDto,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("accessToken", accessToken)
      put("refreshToken", refreshToken)
      put("tokenType", tokenType)
      put("expiresIn", expiresIn)
      put("user", user.toJson())
    }
}
