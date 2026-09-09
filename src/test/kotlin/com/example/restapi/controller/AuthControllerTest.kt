package com.example.restapi.controller

import com.example.restapi.dto.AuthResponse
import com.example.restapi.dto.LoginRequest
import com.example.restapi.dto.SignupRequest
import com.example.restapi.dto.TokenRefreshResponse
import com.example.restapi.dto.UserDto
import com.example.restapi.exception.AppException
import com.example.restapi.security.AuthMiddleware
import com.example.restapi.security.JwtProvider
import com.example.restapi.service.AuthService
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.OffsetDateTime

@ExtendWith(VertxExtension::class)
class AuthControllerTest {
  private lateinit var jwtProvider: JwtProvider
  private lateinit var authMiddleware: AuthMiddleware
  private var serverPort: Int = 0

  // Mock AuthService
  private val mockAuthService =
    object : AuthService {
      override fun signup(request: SignupRequest): Future<AuthResponse> {
        if (request.username == "existing_user") {
          return Future.failedFuture(AppException.ConflictException("Username sudah digunakan"))
        }
        val userDto = UserDto("user-1", request.username, request.email, request.roles, OffsetDateTime.now())
        return Future.succeededFuture(
          AuthResponse("mock-access-token", "mock-refresh-token", "Bearer", 900, userDto),
        )
      }

      override fun login(request: LoginRequest): Future<AuthResponse> {
        if (request.password != "validPassword123") {
          return Future.failedFuture(AppException.UnauthorizedException("Username atau password salah"))
        }
        val userDto = UserDto("user-1", request.usernameOrEmail, "user@example.com", listOf("ROLE_USER"), OffsetDateTime.now())
        return Future.succeededFuture(
          AuthResponse("mock-access-token", "mock-refresh-token", "Bearer", 900, userDto),
        )
      }

      override fun refreshToken(refreshTokenStr: String): Future<TokenRefreshResponse> {
        if (refreshTokenStr == "valid-refresh-token") {
          return Future.succeededFuture(TokenRefreshResponse("new-access-token", "Bearer", 900))
        }
        return Future.failedFuture(AppException.UnauthorizedException("Refresh token tidak valid"))
      }

      override fun getCurrentUser(userId: String): Future<UserDto> =
        Future.succeededFuture(
          UserDto(userId, "testuser", "test@example.com", listOf("ROLE_USER"), OffsetDateTime.now()),
        )
    }

  @BeforeEach
  fun setUp(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    jwtProvider = JwtProvider(vertx, "test-secret-key-that-is-at-least-256-bits-long-54321", 15)
    authMiddleware = AuthMiddleware(jwtProvider)

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())

    val authController = AuthController(mockAuthService, authMiddleware)
    authController.mount(router)

    val rbacDemoController = RbacDemoController(authMiddleware)
    rbacDemoController.mount(router)

    // Global failure handler
    router.route().failureHandler { ctx ->
      val failure = ctx.failure()
      if (failure is AppException) {
        ctx.response().setStatusCode(failure.statusCode).end(
          JsonObject()
            .put("success", false)
            .put("error", failure.error)
            .put("message", failure.message)
            .encode(),
        )
        return@failureHandler
      }
      ctx.response().setStatusCode(500).end(
        JsonObject().put("success", false).put("error", "Internal Server Error").encode(),
      )
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
  @DisplayName("POST /api/v1/auth/signup berhasil membuat user baru dengan status 201")
  fun testSignupSuccess(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    val payload =
      JsonObject().apply {
        put("username", "budi123")
        put("email", "budi@example.com")
        put("password", "rahasia123")
      }

    client
      .post(serverPort, "localhost", "/api/v1/auth/signup")
      .sendJsonObject(payload)
      .onComplete(
        testContext.succeeding { res ->
          testContext.verify {
            assertEquals(201, res.statusCode())
            val body = res.bodyAsJsonObject()
            assertTrue(body.getBoolean("success"))
            val data = body.getJsonObject("data")
            assertNotNull(data.getString("accessToken"))
            assertNotNull(data.getString("refreshToken"))
            assertEquals("budi123", data.getJsonObject("user").getString("username"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("POST /api/v1/auth/signup dengan username yang sudah terdaftar mengembalikan status 409")
  fun testSignupConflict(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    val payload =
      JsonObject().apply {
        put("username", "existing_user")
        put("email", "exist@example.com")
        put("password", "rahasia123")
      }

    client
      .post(serverPort, "localhost", "/api/v1/auth/signup")
      .sendJsonObject(payload)
      .onComplete(
        testContext.succeeding { res ->
          testContext.verify {
            assertEquals(409, res.statusCode())
            val body = res.bodyAsJsonObject()
            assertFalse(body.getBoolean("success"))
            assertEquals("Conflict", body.getString("error"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("POST /api/v1/auth/login berhasil mengembalikan accessToken dan refreshToken")
  fun testLoginSuccess(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    val payload =
      JsonObject().apply {
        put("usernameOrEmail", "budi123")
        put("password", "validPassword123")
      }

    client
      .post(serverPort, "localhost", "/api/v1/auth/login")
      .sendJsonObject(payload)
      .onComplete(
        testContext.succeeding { res ->
          testContext.verify {
            assertEquals(200, res.statusCode())
            val body = res.bodyAsJsonObject()
            assertTrue(body.getBoolean("success"))
            val data = body.getJsonObject("data")
            assertEquals("mock-access-token", data.getString("accessToken"))
            assertEquals("mock-refresh-token", data.getString("refreshToken"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("POST /api/v1/auth/login dengan password salah mengembalikan 401 Unauthorized")
  fun testLoginInvalidPassword(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    val payload =
      JsonObject().apply {
        put("usernameOrEmail", "budi123")
        put("password", "wrongPassword")
      }

    client
      .post(serverPort, "localhost", "/api/v1/auth/login")
      .sendJsonObject(payload)
      .onComplete(
        testContext.succeeding { res ->
          testContext.verify {
            assertEquals(401, res.statusCode())
            val body = res.bodyAsJsonObject()
            assertFalse(body.getBoolean("success"))
            assertEquals("Unauthorized", body.getString("error"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("POST /api/v1/auth/refresh-token berhasil memperbarui accessToken")
  fun testRefreshTokenSuccess(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    val payload =
      JsonObject().apply {
        put("refreshToken", "valid-refresh-token")
      }

    client
      .post(serverPort, "localhost", "/api/v1/auth/refresh-token")
      .sendJsonObject(payload)
      .onComplete(
        testContext.succeeding { res ->
          testContext.verify {
            assertEquals(200, res.statusCode())
            val body = res.bodyAsJsonObject()
            assertTrue(body.getBoolean("success"))
            val data = body.getJsonObject("data")
            assertEquals("new-access-token", data.getString("accessToken"))
            assertEquals("Bearer", data.getString("tokenType"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("GET /api/v1/auth/me berhasil mengambil data user profil saat menyertakan token valid")
  fun testGetProfile(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val token = jwtProvider.generateAccessToken("user-1", "testuser", "test@example.com", listOf("ROLE_USER"))
    val client = WebClient.create(vertx)

    client
      .get(serverPort, "localhost", "/api/v1/auth/me")
      .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer $token")
      .send()
      .onComplete(
        testContext.succeeding { res ->
          testContext.verify {
            assertEquals(200, res.statusCode())
            val body = res.bodyAsJsonObject()
            assertTrue(body.getBoolean("success"))
            val data = body.getJsonObject("data")
            assertEquals("user-1", data.getString("id"))
            assertEquals("testuser", data.getString("username"))
            testContext.completeNow()
          }
        },
      )
  }
}
