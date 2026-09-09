package com.example.restapi.repository

import com.example.restapi.db.DatabaseManager
import com.example.restapi.model.RefreshToken
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.jooq.Field
import org.jooq.Record
import org.jooq.Table
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.Optional

class RefreshTokenRepositoryImpl(
  private val vertx: Vertx,
  private val databaseManager: DatabaseManager,
) : RefreshTokenRepository {
  override fun save(token: RefreshToken): Future<RefreshToken> =
    vertx.executeBlocking<RefreshToken> {
      databaseManager.dsl
        .insertInto(REFRESH_TOKENS)
        .set(ID, token.id)
        .set(USER_ID, token.userId)
        .set(TOKEN, token.token)
        .set(EXPIRES_AT, token.expiresAt)
        .set(REVOKED, token.revoked)
        .set(CREATED_AT, token.createdAt ?: OffsetDateTime.now())
        .execute()
      token
    }

  override fun findByToken(token: String): Future<Optional<RefreshToken>> =
    vertx.executeBlocking<Optional<RefreshToken>> {
      val record =
        databaseManager.dsl
          .select(ID, USER_ID, TOKEN, EXPIRES_AT, REVOKED, CREATED_AT)
          .from(REFRESH_TOKENS)
          .where(TOKEN.eq(token))
          .fetchOne()

      if (record != null) {
        Optional.of(
          RefreshToken(
            id = record.get(ID),
            userId = record.get(USER_ID),
            token = record.get(TOKEN),
            expiresAt = record.get(EXPIRES_AT),
            revoked = record.get(REVOKED) ?: false,
            createdAt = record.get(CREATED_AT),
          ),
        )
      } else {
        Optional.empty()
      }
    }

  override fun revokeByToken(token: String): Future<Boolean> =
    vertx.executeBlocking<Boolean> {
      val updated =
        databaseManager.dsl
          .update(REFRESH_TOKENS)
          .set(REVOKED, true)
          .where(TOKEN.eq(token))
          .execute()
      updated > 0
    }

  override fun revokeByUserId(userId: String): Future<Boolean> =
    vertx.executeBlocking<Boolean> {
      val updated =
        databaseManager.dsl
          .update(REFRESH_TOKENS)
          .set(REVOKED, true)
          .where(USER_ID.eq(userId))
          .execute()
      updated > 0
    }

  companion object {
    val REFRESH_TOKENS: Table<Record> = DSL.table("refresh_tokens")
    val ID: Field<String> = DSL.field("id", String::class.java)
    val USER_ID: Field<String> = DSL.field("user_id", String::class.java)
    val TOKEN: Field<String> = DSL.field("token", String::class.java)
    val EXPIRES_AT: Field<OffsetDateTime> = DSL.field("expires_at", OffsetDateTime::class.java)
    val REVOKED: Field<Boolean> = DSL.field("revoked", Boolean::class.java)
    val CREATED_AT: Field<OffsetDateTime> = DSL.field("created_at", OffsetDateTime::class.java)
  }
}
