package com.example.restapi.controller

import com.example.restapi.dto.CreateItemRequest
import com.example.restapi.exception.AppException
import com.example.restapi.service.ItemService
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class ItemController(
  private val itemService: ItemService
) {
  fun mount(router: Router) {
    router.get("/api/v1/items").handler(this::getAll)
    router.get("/api/v1/items/:id").handler(this::getById)
    router.post("/api/v1/items").handler(this::create)
    router.delete("/api/v1/items/:id").handler(this::delete)
  }

  fun getAll(ctx: RoutingContext) {
    itemService.getAllItems()
      .onSuccess { items ->
        val array = JsonArray()
        items.forEach { array.add(it.toJson()) }
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end(array.encodePrettily())
      }
      .onFailure(ctx::fail)
  }

  fun getById(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    itemService.getItemById(id)
      .onSuccess { item ->
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end(item.toJson().encodePrettily())
      }
      .onFailure(ctx::fail)
  }

  fun create(ctx: RoutingContext) {
    val body: JsonObject?
    try {
      body = ctx.body().asJsonObject()
    } catch (e: DecodeException) {
      ctx.response()
        .setStatusCode(400)
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(
          JsonObject()
            .put("error", "Bad Request")
            .put("message", "Format JSON request tidak valid")
            .encode()
        )
      return
    }

    try {
      val request = CreateItemRequest.fromJson(body)
      itemService.createItem(request)
        .onSuccess { created ->
          ctx.response()
            .setStatusCode(201)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(created.toJson().encodePrettily())
        }
        .onFailure(ctx::fail)
    } catch (e: AppException.BadRequestException) {
      ctx.response()
        .setStatusCode(400)
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(
          JsonObject()
            .put("error", "Bad Request")
            .put("message", e.message)
            .encode()
        )
    }
  }

  fun delete(ctx: RoutingContext) {
    val id = ctx.pathParam("id")
    itemService.deleteItem(id)
      .onSuccess {
        ctx.response()
          .setStatusCode(200)
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end(
            JsonObject()
              .put("message", "Item berhasil dihapus")
              .put("id", id)
              .encode()
          )
      }
      .onFailure(ctx::fail)
  }
}
