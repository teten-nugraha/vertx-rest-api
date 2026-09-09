package com.example.restapi.exception

open class AppException(
  val statusCode: Int,
  override val message: String,
  val error: String? = null,
  val errors: List<String>? = null
) : RuntimeException(message) {
  class NotFoundException(message: String = "Resource tidak ditemukan") :
    AppException(404, message, "Not Found")

  class BadRequestException(message: String = "Bad Request", errors: List<String>? = null) :
    AppException(400, message, "Bad Request", errors)

  class UnauthorizedException(message: String = "Unauthorized") :
    AppException(401, message, "Unauthorized")

  class ForbiddenException(message: String = "Akses ditolak") :
    AppException(403, message, "Forbidden")

  class ConflictException(message: String = "Terjadi konflik data") :
    AppException(409, message, "Conflict")

  class InternalServerErrorException(message: String = "Terjadi kesalahan pada server") :
    AppException(500, message, "Internal Server Error")
}

