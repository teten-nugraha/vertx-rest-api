package com.example.restapi.model

import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime

data class Item(
  val id: String,
  val name: String,
  val description: String? = null,
  val createdAt: OffsetDateTime? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("id", id)
      put("name", name)
      put("description", description)
      put("createdAt", createdAt?.toString())
    }

  companion object {
    fun fromJson(json: JsonObject): Item {
      val id = json.getString("id")
      val name = json.getString("name")
      val description = json.getString("description")
      val createdAtStr = json.getString("createdAt")
      val createdAt = if (createdAtStr != null) OffsetDateTime.parse(createdAtStr) else null
      return Item(id, name, description, createdAt)
    }
  }
}
