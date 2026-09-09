package com.example.restapi.controller

import com.example.restapi.dto.CreateItemRequest
import com.example.restapi.exception.AppException
import com.example.restapi.response.respondCreated
import com.example.restapi.response.respondError
import com.example.restapi.response.respondSuccess
import com.example.restapi.service.ItemService
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class ItemController(
  private val itemService: ItemService,
) {
  fun mount(router: Router) {
    router.get("/api/v1/items").handler(this::getAll)
    router.get("/api/v1/items/:id").handler(this::getById)
    router.post("/api/v1/items").handler(this::create)
    router.delete("/api/v1/items/:id").handler(this::delete)
  }

  fun getAll(ctx: RoutingContext) {
    itemService
      .getAllItems()
      .onSuccess { items ->
        val array = JsonArray()
        items.forEach { array.add(it.toJson()) }
        ctx.respondSuccess(
          data = array,
          message = "Data item berhasil diambil",
        )
      }.onFailure(ctx::fail)
  }

  fun getById(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    itemService
      .getItemById(id)
      .onSuccess { item ->
        ctx.respondSuccess(
          data = item.toJson(),
          message = "Data item berhasil ditemukan",
        )
      }.onFailure(ctx::fail)
  }

  fun create(ctx: RoutingContext) {
    val body: JsonObject?
    try {
      body = ctx.body().asJsonObject()
    } catch (e: DecodeException) {
      ctx.respondError(
        statusCode = 400,
        message = "Format JSON request tidak valid",
        error = "Bad Request",
      )
      return
    }

    try {
      val request = CreateItemRequest.fromJson(body)
      itemService
        .createItem(request)
        .onSuccess { created ->
          ctx.respondCreated(
            data = created.toJson(),
            message = "Item berhasil dibuat",
          )
        }.onFailure(ctx::fail)
    } catch (e: AppException.BadRequestException) {
      ctx.respondError(e)
    }
  }

  fun delete(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    itemService
      .deleteItem(id)
      .onSuccess {
        ctx.respondSuccess(
          data = JsonObject().put("id", id),
          message = "Item berhasil dihapus",
        )
      }.onFailure(ctx::fail)
  }
}
