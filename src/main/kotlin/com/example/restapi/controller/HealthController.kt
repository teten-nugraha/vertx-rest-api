package com.example.restapi.controller

import com.example.restapi.config.AppConfig
import com.example.restapi.db.DatabaseManager
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import java.time.Instant

class HealthController(
  private val appConfig: AppConfig,
  private val databaseManager: DatabaseManager,
) {
  fun mount(router: Router) {
    router.get("/health").handler(this::check)
  }

  fun check(ctx: RoutingContext) {
    databaseManager
      .checkHealth()
      .onSuccess { dbUp ->
        val response =
          JsonObject().apply {
            put("status", if (dbUp) "UP" else "DEGRADED")
            put("service", "vertx-rest-api")
            put(
              "database",
              JsonObject().apply {
                put("status", if (dbUp) "UP" else "DOWN")
                put("type", "PostgreSQL (jOOQ)")
                put("host", appConfig.dbHost)
                put("port", appConfig.dbPort)
                put("database", appConfig.dbName)
              },
            )
            put("timestamp", Instant.now().toString())
          }
        ctx
          .response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end(response.encodePrettily())
      }.onFailure { err ->
        val response =
          JsonObject().apply {
            put("status", "DOWN")
            put("service", "vertx-rest-api")
            put(
              "database",
              JsonObject().apply {
                put("status", "DOWN")
                put("error", err.message)
              },
            )
            put("timestamp", Instant.now().toString())
          }
        ctx
          .response()
          .setStatusCode(503)
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end(response.encodePrettily())
      }
  }
}
