package com.example.restapi.service

import com.example.restapi.dto.AuthResponse
import com.example.restapi.dto.LoginRequest
import com.example.restapi.dto.SignupRequest
import com.example.restapi.dto.TokenRefreshResponse
import com.example.restapi.dto.UserDto
import com.example.restapi.exception.AppException
import com.example.restapi.model.RefreshToken
import com.example.restapi.model.User
import com.example.restapi.repository.RefreshTokenRepository
import com.example.restapi.repository.UserRepository
import com.example.restapi.security.JwtProvider
import com.example.restapi.security.PasswordHasher
import io.vertx.core.Future
import java.time.OffsetDateTime
import java.util.UUID

class AuthServiceImpl(
  private val userRepository: UserRepository,
  private val refreshTokenRepository: RefreshTokenRepository,
  private val jwtProvider: JwtProvider,
  private val jwtRefreshExpirationDays: Long = 7L,
) : AuthService {
  override fun signup(request: SignupRequest): Future<AuthResponse> {
    request.validate()

    return userRepository
      .existsByUsername(request.username)
      .compose { usernameExists ->
        if (usernameExists) {
          Future.failedFuture(AppException.ConflictException("Username '${request.username}' sudah digunakan"))
        } else {
          userRepository.existsByEmail(request.email)
        }
      }.compose { emailExists ->
        if (emailExists) {
          Future.failedFuture(AppException.ConflictException("Email '${request.email}' sudah terdaftar"))
        } else {
          val userId = UUID.randomUUID().toString()
          val passwordHash = PasswordHasher.hash(request.password)
          val newUser =
            User(
              id = userId,
              username = request.username,
              email = request.email,
              passwordHash = passwordHash,
              createdAt = OffsetDateTime.now(),
            )
          userRepository.save(newUser, request.roles)
        }
      }.compose { savedUser ->
        createAuthSession(savedUser)
      }
  }

  override fun login(request: LoginRequest): Future<AuthResponse> {
    request.validate()

    return userRepository
      .findByUsernameOrEmail(request.usernameOrEmail)
      .compose { optionalUser ->
        if (optionalUser.isEmpty) {
          Future.failedFuture(AppException.UnauthorizedException("Username atau password salah"))
        } else {
          val user = optionalUser.get()
          val passwordMatches = PasswordHasher.verify(request.password, user.passwordHash)
          if (!passwordMatches) {
            Future.failedFuture(AppException.UnauthorizedException("Username atau password salah"))
          } else {
            createAuthSession(user)
          }
        }
      }
  }

  override fun refreshToken(refreshTokenStr: String): Future<TokenRefreshResponse> {
    if (refreshTokenStr.isBlank()) {
      return Future.failedFuture(AppException.BadRequestException("Field 'refreshToken' wajib diisi"))
    }

    return refreshTokenRepository
      .findByToken(refreshTokenStr)
      .compose { optionalToken ->
        if (optionalToken.isEmpty) {
          Future.failedFuture(AppException.UnauthorizedException("Refresh token tidak valid"))
        } else {
          val tokenRecord = optionalToken.get()
          if (tokenRecord.revoked) {
            Future.failedFuture(AppException.UnauthorizedException("Refresh token telah dicabut (revoked)"))
          } else if (tokenRecord.expiresAt.isBefore(OffsetDateTime.now())) {
            Future.failedFuture(AppException.UnauthorizedException("Refresh token sudah kedaluwarsa"))
          } else {
            userRepository
              .findById(tokenRecord.userId)
              .compose { optionalUser ->
                if (optionalUser.isEmpty) {
                  Future.failedFuture(AppException.UnauthorizedException("User tidak ditemukan"))
                } else {
                  val user = optionalUser.get()
                  val newAccessToken =
                    jwtProvider.generateAccessToken(
                      userId = user.id,
                      username = user.username,
                      email = user.email,
                      roles = user.roles,
                    )
                  Future.succeededFuture(
                    TokenRefreshResponse(
                      accessToken = newAccessToken,
                      tokenType = "Bearer",
                      expiresIn = jwtProvider.getAccessExpiresInSeconds(),
                    ),
                  )
                }
              }
          }
        }
      }
  }

  override fun getCurrentUser(userId: String): Future<UserDto> =
    userRepository.findById(userId).compose { optionalUser ->
      if (optionalUser.isEmpty) {
        Future.failedFuture(AppException.NotFoundException("User dengan ID '$userId' tidak ditemukan"))
      } else {
        Future.succeededFuture(UserDto.fromUser(optionalUser.get()))
      }
    }

  private fun createAuthSession(user: User): Future<AuthResponse> {
    val accessToken =
      jwtProvider.generateAccessToken(
        userId = user.id,
        username = user.username,
        email = user.email,
        roles = user.roles,
      )
    val refreshTokenString = jwtProvider.generateRefreshToken()
    val expiresAt = OffsetDateTime.now().plusDays(jwtRefreshExpirationDays)

    val refreshTokenRecord =
      RefreshToken(
        id = UUID.randomUUID().toString(),
        userId = user.id,
        token = refreshTokenString,
        expiresAt = expiresAt,
        revoked = false,
        createdAt = OffsetDateTime.now(),
      )

    return refreshTokenRepository
      .save(refreshTokenRecord)
      .map {
        AuthResponse(
          accessToken = accessToken,
          refreshToken = refreshTokenString,
          tokenType = "Bearer",
          expiresIn = jwtProvider.getAccessExpiresInSeconds(),
          user = UserDto.fromUser(user),
        )
      }
  }
}
