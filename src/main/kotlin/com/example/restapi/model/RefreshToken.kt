package com.example.restapi.model

import java.time.OffsetDateTime

data class RefreshToken(
  val id: String,
  val userId: String,
  val token: String,
  val expiresAt: OffsetDateTime,
  val revoked: Boolean = false,
  val createdAt: OffsetDateTime? = null,
)
