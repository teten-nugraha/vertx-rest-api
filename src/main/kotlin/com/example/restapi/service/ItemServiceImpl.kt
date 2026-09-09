package com.example.restapi.service

import com.example.restapi.dto.CreateItemRequest
import com.example.restapi.exception.AppException
import com.example.restapi.model.Item
import com.example.restapi.repository.ItemRepository
import io.vertx.core.Future
import java.time.OffsetDateTime
import java.util.UUID

class ItemServiceImpl(
  private val itemRepository: ItemRepository,
) : ItemService {
  override fun getAllItems(): Future<List<Item>> = itemRepository.findAll()

  override fun getItemById(id: String): Future<Item> =
    itemRepository.findById(id).compose { optionalItem ->
      if (optionalItem.isEmpty) {
        Future.failedFuture(AppException.NotFoundException("Item dengan ID '$id' tidak ditemukan"))
      } else {
        Future.succeededFuture(optionalItem.get())
      }
    }

  override fun createItem(request: CreateItemRequest): Future<Item> {
    request.validate()

    val id = UUID.randomUUID().toString()
    val newItem =
      Item(
        id = id,
        name = request.name.trim(),
        description = request.description?.trim() ?: "",
        createdAt = OffsetDateTime.now(),
      )

    return itemRepository.save(newItem)
  }

  override fun deleteItem(id: String): Future<Void> =
    itemRepository.deleteById(id).compose { deleted ->
      if (!deleted) {
        Future.failedFuture(AppException.NotFoundException("Item dengan ID '$id' tidak ditemukan"))
      } else {
        Future.succeededFuture()
      }
    }
}
