package com.example.restapi.repository

import com.example.restapi.model.User
import io.vertx.core.Future
import java.util.Optional

interface UserRepository {
  fun findById(id: String): Future<Optional<User>>

  fun findByUsername(username: String): Future<Optional<User>>

  fun findByEmail(email: String): Future<Optional<User>>

  fun findByUsernameOrEmail(identifier: String): Future<Optional<User>>

  fun existsByUsername(username: String): Future<Boolean>

  fun existsByEmail(email: String): Future<Boolean>

  fun save(
    user: User,
    roleNames: List<String>,
  ): Future<User>

  fun getUserRoles(userId: String): Future<List<String>>
}
