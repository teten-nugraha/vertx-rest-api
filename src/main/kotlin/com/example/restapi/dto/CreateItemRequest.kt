package com.example.restapi.dto

import com.example.restapi.exception.AppException
import io.vertx.core.json.JsonObject

data class CreateItemRequest(
  val name: String,
  val description: String? = ""
) {
  fun validate() {
    if (name.isBlank()) {
      throw AppException.BadRequestException("Field 'name' wajib diisi")
    }
  }

  companion object {
    fun fromJson(json: JsonObject?): CreateItemRequest {
      if (json == null) {
        throw AppException.BadRequestException("Request body tidak boleh kosong")
      }
      val name = json.getString("name") ?: ""
      val description = json.getString("description", "")
      return CreateItemRequest(name, description).also { it.validate() }
    }
  }
}
