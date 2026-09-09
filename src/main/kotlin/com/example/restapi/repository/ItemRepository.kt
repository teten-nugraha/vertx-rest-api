package com.example.restapi.repository

import com.example.restapi.model.Item
import io.vertx.core.Future
import java.util.Optional

interface ItemRepository {
  fun findAll(): Future<List<Item>>

  fun findById(id: String): Future<Optional<Item>>

  fun save(item: Item): Future<Item>

  fun deleteById(id: String): Future<Boolean>
}
