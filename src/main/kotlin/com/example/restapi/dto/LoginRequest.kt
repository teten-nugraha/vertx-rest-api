package com.example.restapi.dto

import com.example.restapi.exception.AppException
import io.vertx.core.json.JsonObject

data class LoginRequest(
  val usernameOrEmail: String,
  val password: String,
) {
  fun validate() {
    val errors = mutableListOf<String>()
    if (usernameOrEmail.isBlank()) {
      errors.add("Field 'usernameOrEmail' wajib diisi")
    }
    if (password.isBlank()) {
      errors.add("Field 'password' wajib diisi")
    }
    if (errors.isNotEmpty()) {
      throw AppException.BadRequestException("Validasi data login gagal", errors)
    }
  }

  companion object {
    fun fromJson(json: JsonObject?): LoginRequest {
      if (json == null) {
        throw AppException.BadRequestException("Request body tidak boleh kosong")
      }
      val usernameOrEmail = (json.getString("usernameOrEmail") ?: json.getString("username") ?: json.getString("email") ?: "").trim()
      val password = json.getString("password", "")
      return LoginRequest(usernameOrEmail, password).also { it.validate() }
    }
  }
}
