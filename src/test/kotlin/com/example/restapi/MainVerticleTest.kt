package com.example.restapi

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class MainVerticleTest {
  @BeforeEach
  fun deployVerticle(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    vertx
      .deployVerticle(MainVerticle())
      .onComplete(testContext.succeedingThenComplete())
  }

  @Test
  @DisplayName("Memastikan endpoint /health merespons status 200 dan database bernilai UP")
  fun testHealthCheck(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    client
      .get(8080, "localhost", "/health")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(200, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertNotNull(body)
            assertEquals("UP", body.getString("status"))
            assertNotNull(body.getJsonObject("database"))
            assertEquals("UP", body.getJsonObject("database").getString("status"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("Memastikan endpoint /api/v1/items merespons JSON standar dengan array list item dari database")
  fun testGetItems(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    client
      .get(8080, "localhost", "/api/v1/items")
      .send()
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(200, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertNotNull(body)
            assertTrue(body.getBoolean("success"))
            assertEquals(200, body.getInteger("statusCode"))
            assertNotNull(body.getJsonArray("data"))
            assertTrue(body.getJsonArray("data").size() >= 0)
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("Memastikan POST /api/v1/items berhasil membuat item baru dengan format standar status 201")
  fun testCreateItem(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    val payload =
      JsonObject().apply {
        put("name", "Keyboard Mechanical")
        put("description", "Switch tactile")
      }

    client
      .post(8080, "localhost", "/api/v1/items")
      .sendJsonObject(payload)
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(201, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertNotNull(body)
            assertTrue(body.getBoolean("success"))
            assertEquals(201, body.getInteger("statusCode"))
            val data = body.getJsonObject("data")
            assertNotNull(data)
            assertNotNull(data.getString("id"))
            assertEquals("Keyboard Mechanical", data.getString("name"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("Memastikan POST /api/v1/items tanpa name merespons status 400 format standar error")
  fun testCreateItemInvalid(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    val payload =
      JsonObject().apply {
        put("description", "Tanpa field name")
      }

    client
      .post(8080, "localhost", "/api/v1/items")
      .sendJsonObject(payload)
      .onComplete(
        testContext.succeeding { response ->
          testContext.verify {
            assertEquals(400, response.statusCode())
            val body = response.bodyAsJsonObject()
            assertNotNull(body)
            assertFalse(body.getBoolean("success"))
            assertEquals(400, body.getInteger("statusCode"))
            assertEquals("Bad Request", body.getString("error"))
            testContext.completeNow()
          }
        },
      )
  }

  @Test
  @DisplayName("Memastikan DELETE /api/v1/items/:id berhasil menghapus item dengan response standar")
  fun testDeleteItem(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    val client = WebClient.create(vertx)
    val payload =
      JsonObject().apply {
        put("name", "Item untuk dihapus")
        put("description", "Temporary")
      }

    client
      .post(8080, "localhost", "/api/v1/items")
      .sendJsonObject(payload)
      .onComplete(
        testContext.succeeding { createRes ->
          val id = createRes.bodyAsJsonObject().getJsonObject("data").getString("id")
          client
            .delete(8080, "localhost", "/api/v1/items/$id")
            .send()
            .onComplete(
              testContext.succeeding { deleteRes ->
                testContext.verify {
                  assertEquals(200, deleteRes.statusCode())
                  val body = deleteRes.bodyAsJsonObject()
                  assertNotNull(body)
                  assertTrue(body.getBoolean("success"))
                  assertEquals("Item berhasil dihapus", body.getString("message"))
                  testContext.completeNow()
                }
              },
            )
        },
      )
  }
}
