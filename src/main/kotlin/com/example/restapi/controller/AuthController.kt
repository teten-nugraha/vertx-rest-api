package com.example.restapi.controller

import com.example.restapi.dto.LoginRequest
import com.example.restapi.dto.RefreshTokenRequest
import com.example.restapi.dto.SignupRequest
import com.example.restapi.exception.AppException
import com.example.restapi.response.respondCreated
import com.example.restapi.response.respondError
import com.example.restapi.response.respondSuccess
import com.example.restapi.security.AuthMiddleware
import com.example.restapi.security.userId
import com.example.restapi.service.AuthService
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class AuthController(
  private val authService: AuthService,
  private val authMiddleware: AuthMiddleware,
) {
  fun mount(router: Router) {
    router.post("/api/v1/auth/signup").handler(this::signup)
    router.post("/api/v1/auth/login").handler(this::login)
    router.post("/api/v1/auth/refresh-token").handler(this::refreshToken)

    // Protected endpoint
    router
      .get("/api/v1/auth/me")
      .handler(authMiddleware.authenticate())
      .handler(this::getProfile)
  }

  fun signup(ctx: RoutingContext) {
    val body: JsonObject?
    try {
      body = ctx.body().asJsonObject()
    } catch (e: DecodeException) {
      ctx.respondError(400, "Format JSON request tidak valid", "Bad Request")
      return
    }

    try {
      val request = SignupRequest.fromJson(body)
      authService
        .signup(request)
        .onSuccess { authResponse ->
          ctx.respondCreated(authResponse.toJson(), "Pendaftaran pengguna berhasil")
        }.onFailure(ctx::fail)
    } catch (e: AppException) {
      ctx.respondError(e)
    }
  }

  fun login(ctx: RoutingContext) {
    val body: JsonObject?
    try {
      body = ctx.body().asJsonObject()
    } catch (e: DecodeException) {
      ctx.respondError(400, "Format JSON request tidak valid", "Bad Request")
      return
    }

    try {
      val request = LoginRequest.fromJson(body)
      authService
        .login(request)
        .onSuccess { authResponse ->
          ctx.respondSuccess(authResponse.toJson(), "Login berhasil")
        }.onFailure(ctx::fail)
    } catch (e: AppException) {
      ctx.respondError(e)
    }
  }

  fun refreshToken(ctx: RoutingContext) {
    val body: JsonObject?
    try {
      body = ctx.body().asJsonObject()
    } catch (e: DecodeException) {
      ctx.respondError(400, "Format JSON request tidak valid", "Bad Request")
      return
    }

    try {
      val request = RefreshTokenRequest.fromJson(body)
      authService
        .refreshToken(request.refreshToken)
        .onSuccess { refreshResponse ->
          ctx.respondSuccess(refreshResponse.toJson(), "Token berhasil diperbarui")
        }.onFailure(ctx::fail)
    } catch (e: AppException) {
      ctx.respondError(e)
    }
  }

  fun getProfile(ctx: RoutingContext) {
    val currentUserId = ctx.userId()
    if (currentUserId == null) {
      ctx.respondError(401, "Sesi autentikasi tidak valid", "Unauthorized")
      return
    }

    authService
      .getCurrentUser(currentUserId)
      .onSuccess { userDto ->
        ctx.respondSuccess(userDto.toJson(), "Data profil berhasil diambil")
      }.onFailure(ctx::fail)
  }
}
