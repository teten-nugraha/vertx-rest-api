package com.example.restapi.response

import com.example.restapi.exception.AppException
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class ResponseExtensionsHttpTest {

  private var serverPort: Int = 0

  @BeforeEach
  fun setUp(vertx: Vertx, testContext: VertxTestContext) {
    val router = Router.router(vertx)

    router.get("/test/success").handler { ctx ->
      ctx.respondSuccess(
        data = JsonObject().put("name", "Vert.x"),
        message = "Operasi sukses"
      )
    }

    router.post("/test/created").handler { ctx ->
      ctx.respondCreated(
        data = JsonObject().put("id", "generated-123"),
        message = "Data berhasil dibuat"
      )
    }

    router.get("/test/bad-request").handler { ctx ->
      ctx.fail(AppException.BadRequestException("Data tidak valid", listOf("Field name wajib diisi")))
    }

    router.get("/test/not-found").handler { ctx ->
      ctx.fail(AppException.NotFoundException("Data item tidak ditemukan"))
    }

    router.get("/test/server-error").handler { ctx ->
      ctx.fail(RuntimeException("Koneksi gagal"))
    }

    // 404 Handler
    router.errorHandler(404) { ctx ->
      ctx.respondError(
        statusCode = 404,
        message = "Endpoint atau resource tidak ditemukan",
        error = "Not Found"
      )
    }

    // Failure Handler
    router.route().failureHandler { ctx ->
      val failure = ctx.failure()
      if (failure is AppException) {
        ctx.respondError(failure)
        return@failureHandler
      }

      val statusCode = if (ctx.statusCode() > 0) ctx.statusCode() else 500
      val message = failure?.message ?: "Internal Server Error"
      ctx.respondError(
        statusCode = statusCode,
        message = message,
        error = "Internal Server Error"
      )
    }

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(0)
      .onComplete(testContext.succeeding { server ->
        serverPort = server.actualPort()
        testContext.completeNow()
      })
  }

  @Test
  @DisplayName("Endpoint success mengembalikan status 200 dengan format ApiResponse standar")
  fun testSuccessHttp(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)
    client.get(serverPort, "localhost", "/test/success")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(200, response.statusCode())
          assertEquals("application/json", response.getHeader("content-type"))
          val body = response.bodyAsJsonObject()
          assertTrue(body.getBoolean("success"))
          assertEquals(200, body.getInteger("statusCode"))
          assertEquals("Operasi sukses", body.getString("message"))
          assertNotNull(body.getString("timestamp"))
          assertEquals("Vert.x", body.getJsonObject("data").getString("name"))
          testContext.completeNow()
        }
      })
  }

  @Test
  @DisplayName("Endpoint created mengembalikan status 201 dengan format ApiResponse standar")
  fun testCreatedHttp(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)
    client.post(serverPort, "localhost", "/test/created")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(201, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertTrue(body.getBoolean("success"))
          assertEquals(201, body.getInteger("statusCode"))
          assertEquals("Data berhasil dibuat", body.getString("message"))
          assertEquals("generated-123", body.getJsonObject("data").getString("id"))
          testContext.completeNow()
        }
      })
  }

  @Test
  @DisplayName("Endpoint bad-request mengembalikan status 400 dengan error title dan rincian errors")
  fun testBadRequestHttp(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)
    client.get(serverPort, "localhost", "/test/bad-request")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(400, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertFalse(body.getBoolean("success"))
          assertEquals(400, body.getInteger("statusCode"))
          assertEquals("Data tidak valid", body.getString("message"))
          assertEquals("Bad Request", body.getString("error"))
          assertEquals(1, body.getJsonArray("errors").size())
          assertEquals("Field name wajib diisi", body.getJsonArray("errors").getString(0))
          testContext.completeNow()
        }
      })
  }

  @Test
  @DisplayName("Endpoint not-found mengembalikan status 404 dengan pesan yang sesuai")
  fun testNotFoundHttp(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)
    client.get(serverPort, "localhost", "/test/not-found")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(404, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertFalse(body.getBoolean("success"))
          assertEquals(404, body.getInteger("statusCode"))
          assertEquals("Data item tidak ditemukan", body.getString("message"))
          assertEquals("Not Found", body.getString("error"))
          testContext.completeNow()
        }
      })
  }

  @Test
  @DisplayName("URL tidak terdaftar ditangani oleh 404 handler dengan standar ApiResponse")
  fun testUnhandledRoute404(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)
    client.get(serverPort, "localhost", "/unknown-route")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(404, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertFalse(body.getBoolean("success"))
          assertEquals(404, body.getInteger("statusCode"))
          assertEquals("Endpoint atau resource tidak ditemukan", body.getString("message"))
          assertEquals("Not Found", body.getString("error"))
          testContext.completeNow()
        }
      })
  }

  @Test
  @DisplayName("Server error yang tidak terduga ditangani oleh global failureHandler dengan status 500")
  fun testServerErrorHttp(vertx: Vertx, testContext: VertxTestContext) {
    val client = WebClient.create(vertx)
    client.get(serverPort, "localhost", "/test/server-error")
      .send()
      .onComplete(testContext.succeeding { response ->
        testContext.verify {
          assertEquals(500, response.statusCode())
          val body = response.bodyAsJsonObject()
          assertFalse(body.getBoolean("success"))
          assertEquals(500, body.getInteger("statusCode"))
          assertEquals("Koneksi gagal", body.getString("message"))
          assertEquals("Internal Server Error", body.getString("error"))
          testContext.completeNow()
        }
      })
  }
}
