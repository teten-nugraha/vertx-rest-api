package com.example.restapi.dto

import com.example.restapi.exception.AppException
import io.vertx.core.json.JsonObject

data class RefreshTokenRequest(
  val refreshToken: String,
) {
  fun validate() {
    if (refreshToken.isBlank()) {
      throw AppException.BadRequestException("Field 'refreshToken' wajib diisi")
    }
  }

  companion object {
    fun fromJson(json: JsonObject?): RefreshTokenRequest {
      if (json == null) {
        throw AppException.BadRequestException("Request body tidak boleh kosong")
      }
      val token = json.getString("refreshToken", "").trim()
      return RefreshTokenRequest(token).also { it.validate() }
    }
  }
}
