package com.example.restapi.dto

import com.example.restapi.exception.AppException
import io.vertx.core.json.JsonObject

data class SignupRequest(
  val username: String,
  val email: String,
  val password: String,
  val roles: List<String> = listOf("ROLE_USER"),
) {
  fun validate() {
    val errors = mutableListOf<String>()

    if (username.isBlank() || username.length < 3) {
      errors.add("Field 'username' minimal 3 karakter")
    }
    if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
      errors.add("Format 'email' tidak valid")
    }
    if (password.isBlank() || password.length < 6) {
      errors.add("Field 'password' minimal 6 karakter")
    }

    if (errors.isNotEmpty()) {
      throw AppException.BadRequestException("Validasi data pendaftaran gagal", errors)
    }
  }

  companion object {
    fun fromJson(json: JsonObject?): SignupRequest {
      if (json == null) {
        throw AppException.BadRequestException("Request body tidak boleh kosong")
      }
      val username = json.getString("username", "").trim()
      val email = json.getString("email", "").trim().lowercase()
      val password = json.getString("password", "")

      val rolesArray = json.getJsonArray("roles")
      val roles =
        if (rolesArray != null && !rolesArray.isEmpty) {
          rolesArray
            .map { it.toString().trim().uppercase() }
            .map { if (it.startsWith("ROLE_")) it else "ROLE_$it" }
        } else {
          listOf("ROLE_USER")
        }

      return SignupRequest(username, email, password, roles).also { it.validate() }
    }
  }
}
