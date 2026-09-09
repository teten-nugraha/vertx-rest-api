package com.example.restapi.security

import com.example.restapi.response.respondError
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext

class AuthMiddleware(
  private val jwtProvider: JwtProvider,
) {
  /**
   * Middleware to verify JWT Bearer token in the Authorization header.
   */
  fun authenticate(): Handler<RoutingContext> {
    return Handler { ctx ->
      val authHeader = ctx.request().getHeader(HttpHeaders.AUTHORIZATION)
      if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ", ignoreCase = true)) {
        ctx.respondError(
          statusCode = 401,
          message = "Header 'Authorization: Bearer <token>' diperlukan",
          error = "Unauthorized",
        )
        return@Handler
      }

      val token = authHeader.substring(7).trim()
      jwtProvider
        .verifyAccessToken(token)
        .onSuccess { principal ->
          val userId = principal.getString("sub")
          val username = principal.getString("username")
          val email = principal.getString("email")
          val rolesArray = principal.getJsonArray("roles")
          val roles = rolesArray?.map { it.toString() } ?: emptyList()

          ctx.put("userId", userId)
          ctx.put("username", username)
          ctx.put("email", email)
          ctx.put("roles", roles)
          ctx.put("principal", principal)

          ctx.next()
        }.onFailure {
          ctx.respondError(
            statusCode = 401,
            message = "Token autentikasi tidak valid atau sudah kedaluwarsa",
            error = "Unauthorized",
          )
        }
    }
  }

  /**
   * Middleware to enforce Role-Based Access Control (RBAC).
   * Checks if the authenticated user has at least one of the required roles.
   */
  fun requireRoles(vararg requiredRoles: String): Handler<RoutingContext> {
    return Handler { ctx ->
      val userRoles = ctx.get<List<String>>("roles") ?: emptyList()
      val hasRole = requiredRoles.any { role -> userRoles.contains(role) }

      if (!hasRole) {
        ctx.respondError(
          statusCode = 403,
          message = "Akses ditolak: Endpoint ini memerlukan salah satu role berikut: [${requiredRoles.joinToString(", ")}]",
          error = "Forbidden",
        )
        return@Handler
      }

      ctx.next()
    }
  }
}

fun RoutingContext.userId(): String? = get("userId")

fun RoutingContext.username(): String? = get("username")

fun RoutingContext.userEmail(): String? = get("email")

fun RoutingContext.userRoles(): List<String> = get("roles") ?: emptyList()
