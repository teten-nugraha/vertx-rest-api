package com.example.restapi.controller

import com.example.restapi.response.respondSuccess
import com.example.restapi.security.AuthMiddleware
import com.example.restapi.security.userRoles
import com.example.restapi.security.username
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class RbacDemoController(
  private val authMiddleware: AuthMiddleware,
) {
  fun mount(router: Router) {
    // 1. Endpoint Publik
    router.get("/api/v1/demo/public").handler(this::publicEndpoint)

    // 2. Endpoint Khusus User (ROLE_USER atau ROLE_ADMIN)
    router
      .get("/api/v1/demo/user-only")
      .handler(authMiddleware.authenticate())
      .handler(authMiddleware.requireRoles("ROLE_USER", "ROLE_ADMIN"))
      .handler(this::userOnlyEndpoint)

    // 3. Endpoint Khusus Admin (Hanya ROLE_ADMIN)
    router
      .get("/api/v1/demo/admin-only")
      .handler(authMiddleware.authenticate())
      .handler(authMiddleware.requireRoles("ROLE_ADMIN"))
      .handler(this::adminOnlyEndpoint)
  }

  fun publicEndpoint(ctx: RoutingContext) {
    val data =
      JsonObject().apply {
        put("access", "PUBLIC")
        put("description", "Endpoint ini dapat diakses oleh siapa saja tanpa autentikasi")
      }
    ctx.respondSuccess(data, "Akses publik berhasil")
  }

  fun userOnlyEndpoint(ctx: RoutingContext) {
    val data =
      JsonObject().apply {
        put("access", "USER_LEVEL")
        put("username", ctx.username())
        put("roles", ctx.userRoles())
        put("description", "Endpoint ini memerlukan autentikasi dengan role ROLE_USER atau ROLE_ADMIN")
      }
    ctx.respondSuccess(data, "Akses level user berhasil diverifikasi")
  }

  fun adminOnlyEndpoint(ctx: RoutingContext) {
    val data =
      JsonObject().apply {
        put("access", "ADMIN_LEVEL")
        put("username", ctx.username())
        put("roles", ctx.userRoles())
        put("description", "Endpoint ini hanya dapat diakses oleh pengguna dengan role ROLE_ADMIN")
      }
    ctx.respondSuccess(data, "Akses level admin berhasil diverifikasi")
  }
}
