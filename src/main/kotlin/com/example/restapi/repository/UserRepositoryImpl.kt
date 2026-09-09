package com.example.restapi.repository

import com.example.restapi.db.DatabaseManager
import com.example.restapi.model.User
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.jooq.Field
import org.jooq.Record
import org.jooq.Table
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class UserRepositoryImpl(
  private val vertx: Vertx,
  private val databaseManager: DatabaseManager,
) : UserRepository {
  override fun findById(id: String): Future<Optional<User>> =
    vertx.executeBlocking<Optional<User>> {
      val record =
        databaseManager.dsl
          .select(ID, USERNAME, EMAIL, PASSWORD_HASH, CREATED_AT)
          .from(USERS)
          .where(ID.eq(id))
          .fetchOne()

      if (record != null) {
        val roles = fetchRolesByUserId(id)
        Optional.of(mapRecordToUser(record, roles))
      } else {
        Optional.empty()
      }
    }

  override fun findByUsername(username: String): Future<Optional<User>> =
    vertx.executeBlocking<Optional<User>> {
      val record =
        databaseManager.dsl
          .select(ID, USERNAME, EMAIL, PASSWORD_HASH, CREATED_AT)
          .from(USERS)
          .where(USERNAME.eq(username))
          .fetchOne()

      if (record != null) {
        val roles = fetchRolesByUserId(record.get(ID))
        Optional.of(mapRecordToUser(record, roles))
      } else {
        Optional.empty()
      }
    }

  override fun findByEmail(email: String): Future<Optional<User>> =
    vertx.executeBlocking<Optional<User>> {
      val record =
        databaseManager.dsl
          .select(ID, USERNAME, EMAIL, PASSWORD_HASH, CREATED_AT)
          .from(USERS)
          .where(EMAIL.eq(email))
          .fetchOne()

      if (record != null) {
        val roles = fetchRolesByUserId(record.get(ID))
        Optional.of(mapRecordToUser(record, roles))
      } else {
        Optional.empty()
      }
    }

  override fun findByUsernameOrEmail(identifier: String): Future<Optional<User>> =
    vertx.executeBlocking<Optional<User>> {
      val record =
        databaseManager.dsl
          .select(ID, USERNAME, EMAIL, PASSWORD_HASH, CREATED_AT)
          .from(USERS)
          .where(USERNAME.eq(identifier).or(EMAIL.eq(identifier)))
          .fetchOne()

      if (record != null) {
        val roles = fetchRolesByUserId(record.get(ID))
        Optional.of(mapRecordToUser(record, roles))
      } else {
        Optional.empty()
      }
    }

  override fun existsByUsername(username: String): Future<Boolean> =
    vertx.executeBlocking<Boolean> {
      databaseManager.dsl.fetchExists(
        databaseManager.dsl
          .selectOne()
          .from(USERS)
          .where(USERNAME.eq(username)),
      )
    }

  override fun existsByEmail(email: String): Future<Boolean> =
    vertx.executeBlocking<Boolean> {
      databaseManager.dsl.fetchExists(
        databaseManager.dsl
          .selectOne()
          .from(USERS)
          .where(EMAIL.eq(email)),
      )
    }

  override fun save(
    user: User,
    roleNames: List<String>,
  ): Future<User> =
    vertx.executeBlocking<User> {
      databaseManager.dsl
        .insertInto(USERS)
        .set(ID, user.id)
        .set(USERNAME, user.username)
        .set(EMAIL, user.email)
        .set(PASSWORD_HASH, user.passwordHash)
        .set(CREATED_AT, user.createdAt ?: OffsetDateTime.now())
        .execute()

      val assignedRoles = mutableListOf<String>()
      for (roleName in roleNames) {
        var roleRecord =
          databaseManager.dsl
            .select(ROLE_ID, ROLE_NAME)
            .from(ROLES)
            .where(ROLE_NAME.eq(roleName))
            .fetchOne()

        if (roleRecord == null) {
          val newRoleId = UUID.randomUUID().toString()
          databaseManager.dsl
            .insertInto(ROLES)
            .set(ROLE_ID, newRoleId)
            .set(ROLE_NAME, roleName)
            .onConflict(ROLE_NAME)
            .doNothing()
            .execute()

          roleRecord =
            databaseManager.dsl
              .select(ROLE_ID, ROLE_NAME)
              .from(ROLES)
              .where(ROLE_NAME.eq(roleName))
              .fetchOne()
        }

        if (roleRecord != null) {
          val roleId = roleRecord.get(ROLE_ID)
          databaseManager.dsl
            .insertInto(USER_ROLES)
            .set(UR_USER_ID, user.id)
            .set(UR_ROLE_ID, roleId)
            .onConflict(UR_USER_ID, UR_ROLE_ID)
            .doNothing()
            .execute()
          assignedRoles.add(roleRecord.get(ROLE_NAME))
        }
      }

      user.copy(roles = assignedRoles)
    }

  override fun getUserRoles(userId: String): Future<List<String>> =
    vertx.executeBlocking<List<String>> {
      fetchRolesByUserId(userId)
    }

  private fun fetchRolesByUserId(userId: String): List<String> =
    databaseManager.dsl
      .select(ROLE_NAME)
      .from(USER_ROLES)
      .join(ROLES)
      .on(UR_ROLE_ID.eq(ROLE_ID))
      .where(UR_USER_ID.eq(userId))
      .fetch(ROLE_NAME)

  private fun mapRecordToUser(
    record: Record,
    roles: List<String>,
  ): User =
    User(
      id = record.get(ID),
      username = record.get(USERNAME),
      email = record.get(EMAIL),
      passwordHash = record.get(PASSWORD_HASH),
      createdAt = record.get(CREATED_AT),
      roles = roles,
    )

  companion object {
    val USERS: Table<Record> = DSL.table("users")
    val ID: Field<String> = DSL.field("id", String::class.java)
    val USERNAME: Field<String> = DSL.field("username", String::class.java)
    val EMAIL: Field<String> = DSL.field("email", String::class.java)
    val PASSWORD_HASH: Field<String> = DSL.field("password_hash", String::class.java)
    val CREATED_AT: Field<OffsetDateTime> = DSL.field("created_at", OffsetDateTime::class.java)

    val ROLES: Table<Record> = DSL.table("roles")
    val ROLE_ID: Field<String> = DSL.field("id", String::class.java)
    val ROLE_NAME: Field<String> = DSL.field("name", String::class.java)

    val USER_ROLES: Table<Record> = DSL.table("user_roles")
    val UR_USER_ID: Field<String> = DSL.field("user_id", String::class.java)
    val UR_ROLE_ID: Field<String> = DSL.field("role_id", String::class.java)
  }
}
