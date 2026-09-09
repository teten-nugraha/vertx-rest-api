package com.example.restapi.repository

import com.example.restapi.db.DatabaseManager
import com.example.restapi.model.Role
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.jooq.Field
import org.jooq.Record
import org.jooq.Table
import org.jooq.impl.DSL
import java.util.Optional

class RoleRepositoryImpl(
  private val vertx: Vertx,
  private val databaseManager: DatabaseManager,
) : RoleRepository {
  override fun findByName(name: String): Future<Optional<Role>> =
    vertx.executeBlocking<Optional<Role>> {
      val record =
        databaseManager.dsl
          .select(ID, NAME)
          .from(ROLES)
          .where(NAME.eq(name))
          .fetchOne()

      if (record != null) Optional.of(Role(record.get(ID), record.get(NAME))) else Optional.empty()
    }

  override fun findById(id: String): Future<Optional<Role>> =
    vertx.executeBlocking<Optional<Role>> {
      val record =
        databaseManager.dsl
          .select(ID, NAME)
          .from(ROLES)
          .where(ID.eq(id))
          .fetchOne()

      if (record != null) Optional.of(Role(record.get(ID), record.get(NAME))) else Optional.empty()
    }

  override fun save(role: Role): Future<Role> =
    vertx.executeBlocking<Role> {
      databaseManager.dsl
        .insertInto(ROLES)
        .set(ID, role.id)
        .set(NAME, role.name)
        .onConflict(NAME)
        .doNothing()
        .execute()
      role
    }

  override fun findAll(): Future<List<Role>> =
    vertx.executeBlocking<List<Role>> {
      databaseManager.dsl
        .select(ID, NAME)
        .from(ROLES)
        .fetch()
        .map { Role(it.get(ID), it.get(NAME)) }
    }

  companion object {
    val ROLES: Table<Record> = DSL.table("roles")
    val ID: Field<String> = DSL.field("id", String::class.java)
    val NAME: Field<String> = DSL.field("name", String::class.java)
  }
}
