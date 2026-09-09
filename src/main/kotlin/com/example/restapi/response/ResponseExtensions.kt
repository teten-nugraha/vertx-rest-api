package com.example.restapi.response

import com.example.restapi.exception.AppException
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

/**
 * Sends a JSON response with status code and standard Content-Type header.
 */
fun RoutingContext.respondJson(statusCode: Int, body: JsonObject) {
  if (!response().ended()) {
    response()
      .setStatusCode(statusCode)
      .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
      .end(body.encode())
  }
}

/**
 * Sends a standardized success API response (HTTP 200 OK by default).
 */
fun <T> RoutingContext.respondSuccess(
  data: T? = null,
  message: String = "Operasi berhasil",
  statusCode: Int = 200,
  meta: JsonObject? = null
) {
  val res = ApiResponse.success(data = data, message = message, statusCode = statusCode, meta = meta)
  respondJson(statusCode, res.toJson())
}

/**
 * Sends a standardized resource created response (HTTP 201 Created).
 */
fun <T> RoutingContext.respondCreated(
  data: T? = null,
  message: String = "Resource berhasil dibuat"
) {
  val res = ApiResponse.created(data = data, message = message)
  respondJson(201, res.toJson())
}

/**
 * Sends a standardized error API response with status code, message, error phrase, and optional validation errors.
 */
fun RoutingContext.respondError(
  statusCode: Int,
  message: String,
  error: String? = null,
  errors: List<String>? = null
) {
  val res = ApiResponse.error(statusCode = statusCode, message = message, error = error, errors = errors)
  respondJson(statusCode, res.toJson())
}

/**
 * Sends a standardized error API response directly from an AppException.
 */
fun RoutingContext.respondError(exception: AppException) {
  respondError(
    statusCode = exception.statusCode,
    message = exception.message,
    error = exception.error,
    errors = exception.errors
  )
}
