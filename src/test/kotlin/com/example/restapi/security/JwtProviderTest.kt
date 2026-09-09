package com.example.restapi.security

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class JwtProviderTest {
  private lateinit var jwtProvider: JwtProvider
  private val secret = "test-super-secret-key-at-least-256-bits-long-12345"

  @BeforeEach
  fun setUp(vertx: Vertx) {
    jwtProvider = JwtProvider(vertx, secret, 15)
  }

  @Test
  @DisplayName("JwtProvider menghasilkan access token yang valid dan dapat memverifikasi claims")
  fun testGenerateAndVerifyAccessToken(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val userId = "user-123"
    val username = "john_doe"
    val email = "john@example.com"
    val roles = listOf("ROLE_USER", "ROLE_ADMIN")

    val token = jwtProvider.generateAccessToken(userId, username, email, roles)
    assertNotNull(token)
    assertTrue(token.split(".").size == 3)

    jwtProvider
      .verifyAccessToken(token)
      .onComplete(
        testContext.succeeding { principal ->
          testContext.verify {
            assertEquals(userId, principal.getString("sub"))
            assertEquals(username, principal.getString("username"))
            assertEquals(email, principal.getString("email"))
            val rolesArray = principal.getJsonArray("roles")
            assertEquals(2, rolesArray.size())
            assertTrue(rolesArray.contains("ROLE_USER"))
            assertTrue(rolesArray.contains("ROLE_ADMIN"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("JwtProvider menolak token yang dipalsukan atau diubah (tampered)")
  fun testVerifyTamperedToken(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val token = jwtProvider.generateAccessToken("user-1", "user1", "user1@example.com", listOf("ROLE_USER"))
    val tampered = token.dropLast(4) + "abcd"

    jwtProvider
      .verifyAccessToken(tampered)
      .onComplete(
        testContext.failing { error ->
          testContext.verify {
            assertNotNull(error)
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("JwtProvider menghasilkan refresh token yang unik dan acak")
  fun testGenerateRefreshToken() {
    val token1 = jwtProvider.generateRefreshToken()
    val token2 = jwtProvider.generateRefreshToken()

    assertNotNull(token1)
    assertNotNull(token2)
    assertNotEquals(token1, token2)
    assertTrue(token1.length >= 32)
  }
}
