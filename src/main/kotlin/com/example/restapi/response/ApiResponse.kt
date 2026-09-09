package com.example.restapi.response

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.time.Instant

/**
 * Standard API Response envelope for all REST API endpoints.
 *
 * Success format:
 * {
 *   "success": true,
 *   "statusCode": 200,
 *   "message": "...",
 *   "data": { ... } | [ ... ] | null,
 *   "meta": { ... }, // optional
 *   "timestamp": "2026-09-09T07:50:00Z"
 * }
 *
 * Error format:
 * {
 *   "success": false,
 *   "statusCode": 400,
 *   "message": "...",
 *   "error": "Bad Request",
 *   "errors": [ ... ], // optional
 *   "timestamp": "2026-09-09T07:50:00Z"
 * }
 */
data class ApiResponse<T>(
  val success: Boolean,
  val statusCode: Int,
  val message: String,
  val data: T? = null,
  val meta: JsonObject? = null,
  val error: String? = null,
  val errors: List<String>? = null,
  val timestamp: String = Instant.now().toString()
) {

  fun toJson(): JsonObject {
    val json = JsonObject()
      .put("success", success)
      .put("statusCode", statusCode)
      .put("message", message)

    if (success) {
      when (data) {
        null -> json.putNull("data")
        is JsonObject -> json.put("data", data)
        is JsonArray -> json.put("data", data)
        is List<*> -> {
          val array = JsonArray()
          data.forEach { item ->
            when (item) {
              is JsonObject -> array.add(item)
              is JsonArray -> array.add(item)
              else -> array.add(item)
            }
          }
          json.put("data", array)
        }
        else -> json.put("data", data)
      }

      if (meta != null) {
        json.put("meta", meta)
      }
    } else {
      if (error != null) {
        json.put("error", error)
      }
      if (!errors.isNullOrEmpty()) {
        json.put("errors", JsonArray(errors))
      }
    }

    json.put("timestamp", timestamp)
    return json
  }

  companion object {
    fun <T> success(
      data: T? = null,
      message: String = "Operasi berhasil",
      statusCode: Int = 200,
      meta: JsonObject? = null
    ): ApiResponse<T> {
      return ApiResponse(
        success = true,
        statusCode = statusCode,
        message = message,
        data = data,
        meta = meta
      )
    }

    fun <T> created(
      data: T? = null,
      message: String = "Resource berhasil dibuat"
    ): ApiResponse<T> {
      return ApiResponse(
        success = true,
        statusCode = 201,
        message = message,
        data = data
      )
    }

    fun error(
      statusCode: Int,
      message: String,
      error: String? = null,
      errors: List<String>? = null
    ): ApiResponse<Nothing> {
      return ApiResponse(
        success = false,
        statusCode = statusCode,
        message = message,
        error = error ?: defaultErrorPhrase(statusCode),
        errors = errors
      )
    }

    fun badRequest(
      message: String = "Bad Request",
      errors: List<String>? = null
    ): ApiResponse<Nothing> = error(400, message, "Bad Request", errors)

    fun unauthorized(
      message: String = "Unauthorized"
    ): ApiResponse<Nothing> = error(401, message, "Unauthorized")

    fun forbidden(
      message: String = "Forbidden"
    ): ApiResponse<Nothing> = error(403, message, "Forbidden")

    fun notFound(
      message: String = "Resource tidak ditemukan"
    ): ApiResponse<Nothing> = error(404, message, "Not Found")

    fun conflict(
      message: String = "Conflict"
    ): ApiResponse<Nothing> = error(409, message, "Conflict")

    fun internalServerError(
      message: String = "Terjadi kesalahan pada server"
    ): ApiResponse<Nothing> = error(500, message, "Internal Server Error")

    private fun defaultErrorPhrase(statusCode: Int): String {
      return when (statusCode) {
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        409 -> "Conflict"
        422 -> "Unprocessable Entity"
        500 -> "Internal Server Error"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        else -> if (statusCode in 400..499) "Client Error" else "Server Error"
      }
    }
  }
}
