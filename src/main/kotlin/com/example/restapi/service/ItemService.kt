package com.example.restapi.service

import com.example.restapi.dto.CreateItemRequest
import com.example.restapi.model.Item
import io.vertx.core.Future

interface ItemService {
  fun getAllItems(): Future<List<Item>>
  fun getItemById(id: String): Future<Item>
  fun createItem(request: CreateItemRequest): Future<Item>
  fun deleteItem(id: String): Future<Void>
}
