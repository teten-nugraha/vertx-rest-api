package com.example.restapi.security

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import java.security.SecureRandom
import java.util.Base64

class JwtProvider(
  vertx: Vertx,
  secret: String,
  private val accessTokenExpirationMinutes: Long = 15L,
) {
  private val jwtAuth: JWTAuth

  init {
    val keyOptions =
      PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer(secret)

    val options = JWTAuthOptions().addPubSecKey(keyOptions)
    this.jwtAuth = JWTAuth.create(vertx, options)
  }

  fun generateAccessToken(
    userId: String,
    username: String,
    email: String,
    roles: List<String>,
  ): String {
    val claims =
      JsonObject().apply {
        put("sub", userId)
        put("username", username)
        put("email", email)
        put("roles", JsonArray(roles))
      }

    val options =
      JWTOptions().apply {
        algorithm = "HS256"
        expiresInSeconds = (accessTokenExpirationMinutes * 60).toInt()
      }

    return jwtAuth.generateToken(claims, options)
  }

  fun generateRefreshToken(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }

  fun verifyAccessToken(token: String): Future<JsonObject> =
    jwtAuth
      .authenticate(TokenCredentials(token))
      .map { user -> user.principal() }

  fun getAccessExpiresInSeconds(): Long = accessTokenExpirationMinutes * 60L
}
