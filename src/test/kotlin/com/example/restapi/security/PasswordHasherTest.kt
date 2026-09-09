package com.example.restapi.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PasswordHasherTest {
  @Test
  @DisplayName("PasswordHasher meng-hash password dan berhasil memverifikasi password yang benar")
  fun testHashAndVerify() {
    val plain = "SecretPassword123"
    val hashed = PasswordHasher.hash(plain)

    assertNotEquals(plain, hashed)
    assertTrue(hashed.startsWith("$2a$") || hashed.startsWith("$2b$") || hashed.startsWith("$2y$"))

    assertTrue(PasswordHasher.verify(plain, hashed))
    assertFalse(PasswordHasher.verify("WrongPassword", hashed))
    assertFalse(PasswordHasher.verify("", hashed))
  }
}
