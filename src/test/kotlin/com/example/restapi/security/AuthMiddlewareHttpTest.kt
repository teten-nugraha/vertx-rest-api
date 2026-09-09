package com.example.restapi.security

import com.example.restapi.response.respondSuccess
import io.vertx.core.Vertx
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class AuthMiddlewareHttpTest {
  private lateinit var jwtProvider: JwtProvider
  private lateinit var authMiddleware: AuthMiddleware
  private var serverPort: Int = 0

  @BeforeEach
  fun setUp(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    jwtProvider = JwtProvider(vertx, "test-secret-key-that-is-at-least-256-bits-long-54321", 15)
    authMiddleware = AuthMiddleware(jwtProvider)

    val router = Router.router(vertx)

    // Route Publik
    router.get("/api/v1/test/public").handler { ctx ->
      ctx.respondSuccess(JsonObject().put("type", "public"), "Public access")
    }

    // Route Terproteksi (Profil)
    router
      .get("/api/v1/test/profile")
      .handler(authMiddleware.authenticate())
      .handler { ctx ->
        ctx.respondSuccess(
          JsonObject()
            .put("userId", ctx.userId())
            .put("username", ctx.username())
            .put("roles", ctx.userRoles()),
          "Profile access granted",
        )
      }

    // Route RBAC Khusus Admin
    router
      .get("/api/v1/test/admin-only")
      .handler(authMiddleware.authenticate())
      .handler(authMiddleware.requireRoles("ROLE_ADMIN"))
      .handler { ctx ->
        ctx.respondSuccess(JsonObject().put("access", "ADMIN_GRANTED"), "Admin access granted")
      }

    // Route RBAC User atau Admin
    router
      .get("/api/v1/test/user-or-admin")
      .handler(authMiddleware.authenticate())
      .handler(authMiddleware.requireRoles("ROLE_USER", "ROLE_ADMIN"))
      .handler { ctx ->
        ctx.respondSuccess(JsonObject().put("access", "USER_OR_ADMIN_GRANTED"), "Access granted")
      }

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(0)
      .onComplete(
        testContext.succeeding { server ->
          serverPort = server.actualPort()
          testContext.completeNow()
        },
      )
  }

  @Test
  @DisplayName("Endpoint publik dapat diakses tanpa token Authorization")
  fun testPublicEndpoint(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    client
      .get(serverPort, "localhost", "/api/v1/test/public")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(200, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertTrue(body.getBoolean("success"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("Endpoint terproteksi mengembalikan 401 Unauthorized jika tidak menyertakan Authorization Bearer")
  fun testProtectedEndpointWithoutToken(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    client
      .get(serverPort, "localhost", "/api/v1/test/profile")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(401, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertFalse(body.getBoolean("success"))
            assertEquals(401, body.getInteger("statusCode"))
            assertEquals("Unauthorized", body.getString("error"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("Endpoint terproteksi mengembalikan 401 Unauthorized jika token tidak valid")
  fun testProtectedEndpointWithInvalidToken(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    client
      .get(serverPort, "localhost", "/api/v1/test/profile")
      .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer invalid.token.value")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(401, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertFalse(body.getBoolean("success"))
            assertEquals("Unauthorized", body.getString("error"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("Endpoint terproteksi berhasil diakses dengan token valid")
  fun testProtectedEndpointWithValidToken(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val token = jwtProvider.generateAccessToken("user-1", "alice", "alice@example.com", listOf("ROLE_USER"))
    val client = WebClient.create(vertx)

    client
      .get(serverPort, "localhost", "/api/v1/test/profile")
      .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer $token")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(200, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertTrue(body.getBoolean("success"))
            val data = body.getJsonObject("data")
            assertEquals("user-1", data.getString("userId"))
            assertEquals("alice", data.getString("username"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("RBAC: User dengan ROLE_USER ditolak (403 Forbidden) saat mengakses endpoint khusus ROLE_ADMIN")
  fun testRbacForbiddenForUserRole(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val userToken = jwtProvider.generateAccessToken("user-1", "user_biasa", "user@example.com", listOf("ROLE_USER"))
    val client = WebClient.create(vertx)

    client
      .get(serverPort, "localhost", "/api/v1/test/admin-only")
      .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer $userToken")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(403, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertFalse(body.getBoolean("success"))
            assertEquals(403, body.getInteger("statusCode"))
            assertEquals("Forbidden", body.getString("error"))
            assertTrue(body.getString("message").contains("ROLE_ADMIN"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("RBAC: User dengan ROLE_ADMIN diizinkan (200 OK) mengakses endpoint khusus ROLE_ADMIN")
  fun testRbacSuccessForAdminRole(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val adminToken = jwtProvider.generateAccessToken("admin-1", "superadmin", "admin@example.com", listOf("ROLE_ADMIN"))
    val client = WebClient.create(vertx)

    client
      .get(serverPort, "localhost", "/api/v1/test/admin-only")
      .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer $adminToken")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(200, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertTrue(body.getBoolean("success"))
            assertEquals(200, body.getInteger("statusCode"))
            assertEquals("ADMIN_GRANTED", body.getJsonObject("data").getString("access"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("RBAC: Endpoint dengan multiple allowed roles mengizinkan ROLE_USER")
  fun testRbacMultipleAllowedRoles(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val userToken = jwtProvider.generateAccessToken("user-2", "bob", "bob@example.com", listOf("ROLE_USER"))
    val client = WebClient.create(vertx)

    client
      .get(serverPort, "localhost", "/api/v1/test/user-or-admin")
      .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer $userToken")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(200, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertTrue(body.getBoolean("success"))
            testContext.completeNow()
          }
        },
      )
  }
}
