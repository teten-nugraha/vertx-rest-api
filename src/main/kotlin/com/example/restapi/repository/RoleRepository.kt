package com.example.restapi.repository

import com.example.restapi.model.Role
import io.vertx.core.Future
import java.util.Optional

interface RoleRepository {
  fun findByName(name: String): Future<Optional<Role>>

  fun findById(id: String): Future<Optional<Role>>

  fun save(role: Role): Future<Role>

  fun findAll(): Future<List<Role>>
}
