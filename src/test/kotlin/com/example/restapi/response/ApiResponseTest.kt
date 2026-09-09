package com.example.restapi.response

import com.example.restapi.exception.AppException
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ApiResponseTest {
  @Test
  @DisplayName("Success response dengan object data menghasilkan format JSON yang sesuai")
  fun testSuccessWithObjectData() {
    val data = JsonObject().put("id", "123").put("name", "Keyboard Mechanical")
    val response = ApiResponse.success(data = data, message = "Item ditemukan")
    val json = response.toJson()

    assertTrue(json.getBoolean("success"))
    assertEquals(200, json.getInteger("statusCode"))
    assertEquals("Item ditemukan", json.getString("message"))
    assertNotNull(json.getString("timestamp"))
    assertNotNull(json.getJsonObject("data"))
    assertEquals("123", json.getJsonObject("data").getString("id"))
    assertEquals("Keyboard Mechanical", json.getJsonObject("data").getString("name"))
    assertFalse(json.containsKey("error"))
    assertFalse(json.containsKey("errors"))
  }

  @Test
  @DisplayName("Success response dengan list / array data menghasilkan format JSON yang sesuai")
  fun testSuccessWithArrayData() {
    val items =
      listOf(
        JsonObject().put("id", "1").put("name", "Item 1"),
        JsonObject().put("id", "2").put("name", "Item 2"),
      )
    val meta = JsonObject().put("total", 2).put("page", 1)
    val response = ApiResponse.success(data = items, message = "Daftar item berhasil diambil", meta = meta)
    val json = response.toJson()

    assertTrue(json.getBoolean("success"))
    assertEquals(200, json.getInteger("statusCode"))
    assertEquals(2, json.getJsonArray("data").size())
    assertEquals(2, json.getJsonObject("meta").getInteger("total"))
    assertEquals(1, json.getJsonObject("meta").getInteger("page"))
  }

  @Test
  @DisplayName("Success response dengan null data menghasilkan null pada field data")
  fun testSuccessWithNullData() {
    val response = ApiResponse.success<Any>(data = null, message = "Operasi berhasil tanpa data")
    val json = response.toJson()

    assertTrue(json.getBoolean("success"))
    assertEquals(200, json.getInteger("statusCode"))
    assertTrue(json.containsKey("data"))
    assertNull(json.getValue("data"))
  }

  @Test
  @DisplayName("Created response menghasilkan status code 201")
  fun testCreatedResponse() {
    val data = JsonObject().put("id", "generated-uuid")
    val response = ApiResponse.created(data = data, message = "Item baru berhasil dibuat")
    val json = response.toJson()

    assertTrue(json.getBoolean("success"))
    assertEquals(201, json.getInteger("statusCode"))
    assertEquals("Item baru berhasil dibuat", json.getString("message"))
    assertEquals("generated-uuid", json.getJsonObject("data").getString("id"))
  }

  @Test
  @DisplayName("Error response 400 Bad Request dengan rincian errors list")
  fun testBadRequestWithErrors() {
    val validationErrors = listOf("Field 'name' wajib diisi", "Field 'price' harus berupa angka positif")
    val response =
      ApiResponse.badRequest(
        message = "Validasi gagal",
        errors = validationErrors,
      )
    val json = response.toJson()

    assertFalse(json.getBoolean("success"))
    assertEquals(400, json.getInteger("statusCode"))
    assertEquals("Validasi gagal", json.getString("message"))
    assertEquals("Bad Request", json.getString("error"))
    assertNotNull(json.getJsonArray("errors"))
    assertEquals(2, json.getJsonArray("errors").size())
    assertEquals("Field 'name' wajib diisi", json.getJsonArray("errors").getString(0))
    assertFalse(json.containsKey("data"))
  }

  @Test
  @DisplayName("Error response 404 Not Found")
  fun testNotFoundResponse() {
    val response = ApiResponse.notFound("Item dengan ID 999 tidak ditemukan")
    val json = response.toJson()

    assertFalse(json.getBoolean("success"))
    assertEquals(404, json.getInteger("statusCode"))
    assertEquals("Item dengan ID 999 tidak ditemukan", json.getString("message"))
    assertEquals("Not Found", json.getString("error"))
  }

  @Test
  @DisplayName("Error response 500 Internal Server Error")
  fun testInternalServerErrorResponse() {
    val response = ApiResponse.internalServerError()
    val json = response.toJson()

    assertFalse(json.getBoolean("success"))
    assertEquals(500, json.getInteger("statusCode"))
    assertEquals("Terjadi kesalahan pada server", json.getString("message"))
    assertEquals("Internal Server Error", json.getString("error"))
  }

  @Test
  @DisplayName("AppException subclass terpetakan dengan baik")
  fun testAppExceptionMapping() {
    val ex = AppException.BadRequestException("Field 'name' wajib diisi", listOf("name is required"))
    assertEquals(400, ex.statusCode)
    assertEquals("Field 'name' wajib diisi", ex.message)
    assertEquals("Bad Request", ex.error)
    assertEquals(1, ex.errors?.size)

    val notFound = AppException.NotFoundException("Data tidak ditemukan")
    assertEquals(404, notFound.statusCode)
    assertEquals("Not Found", notFound.error)
  }
}
