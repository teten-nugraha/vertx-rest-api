package com.example.restapi.dto

import io.vertx.core.json.JsonObject

data class TokenRefreshResponse(
  val accessToken: String,
  val tokenType: String = "Bearer",
  val expiresIn: Long,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("accessToken", accessToken)
      put("tokenType", tokenType)
      put("expiresIn", expiresIn)
    }
}
