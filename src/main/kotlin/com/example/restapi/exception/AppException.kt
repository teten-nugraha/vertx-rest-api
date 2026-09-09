package com.example.restapi.exception

open class AppException(val statusCode: Int, message: String) : RuntimeException(message) {
  class NotFoundException(message: String) : AppException(404, message)
  class BadRequestException(message: String) : AppException(400, message)
}
