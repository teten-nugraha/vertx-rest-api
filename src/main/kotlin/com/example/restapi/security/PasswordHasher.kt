package com.example.restapi.security

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
  fun hash(plainText: String): String = BCrypt.hashpw(plainText, BCrypt.gensalt(12))

  fun verify(
    plainText: String,
    hashed: String,
  ): Boolean =
    try {
      BCrypt.checkpw(plainText, hashed)
    } catch (e: Exception) {
      false
    }
}
