package com.example.restapi.repository

import com.example.restapi.db.DatabaseManager
import com.example.restapi.model.Item
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.jooq.Field
import org.jooq.Record
import org.jooq.Table
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.Optional

class ItemRepositoryImpl(
  private val vertx: Vertx,
  private val databaseManager: DatabaseManager
) : ItemRepository {

  override fun findAll(): Future<List<Item>> {
    return vertx.executeBlocking<List<Item>> {
      val records = databaseManager.dsl
        .select(ID, NAME, DESCRIPTION, CREATED_AT)
        .from(ITEMS)
        .orderBy(CREATED_AT.desc())
        .fetch()

      records.map { mapRecordToItem(it) }
    }
  }

  override fun findById(id: String): Future<Optional<Item>> {
    return vertx.executeBlocking<Optional<Item>> {
      val record = databaseManager.dsl
        .select(ID, NAME, DESCRIPTION, CREATED_AT)
        .from(ITEMS)
        .where(ID.eq(id))
        .fetchOne()

      if (record != null) Optional.of(mapRecordToItem(record)) else Optional.empty()
    }
  }

  override fun save(item: Item): Future<Item> {
    return vertx.executeBlocking<Item> {
      val record = databaseManager.dsl
        .insertInto(ITEMS)
        .set(ID, item.id)
        .set(NAME, item.name)
        .set(DESCRIPTION, item.description)
        .returning(ID, NAME, DESCRIPTION, CREATED_AT)
        .fetchOne()

      if (record != null) mapRecordToItem(record) else item
    }
  }

  override fun deleteById(id: String): Future<Boolean> {
    return vertx.executeBlocking<Boolean> {
      val deletedRows = databaseManager.dsl
        .deleteFrom(ITEMS)
        .where(ID.eq(id))
        .execute()

      deletedRows > 0
    }
  }

  private fun mapRecordToItem(record: Record): Item {
    return Item(
      id = record.get(ID),
      name = record.get(NAME),
      description = record.get(DESCRIPTION),
      createdAt = record.get(CREATED_AT)
    )
  }

  companion object {
    val ITEMS: Table<Record> = DSL.table("items")
    val ID: Field<String> = DSL.field("id", String::class.java)
    val NAME: Field<String> = DSL.field("name", String::class.java)
    val DESCRIPTION: Field<String> = DSL.field("description", String::class.java)
    val CREATED_AT: Field<OffsetDateTime> = DSL.field("created_at", OffsetDateTime::class.java)
  }
}
