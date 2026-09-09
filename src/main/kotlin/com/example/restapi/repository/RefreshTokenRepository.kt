package com.example.restapi.repository

import com.example.restapi.model.RefreshToken
import io.vertx.core.Future
import java.util.Optional

interface RefreshTokenRepository {
  fun save(token: RefreshToken): Future<RefreshToken>

  fun findByToken(token: String): Future<Optional<RefreshToken>>

  fun revokeByToken(token: String): Future<Boolean>

  fun revokeByUserId(userId: String): Future<Boolean>
}
