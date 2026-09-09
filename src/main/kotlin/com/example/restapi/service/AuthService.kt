package com.example.restapi.service

import com.example.restapi.dto.AuthResponse
import com.example.restapi.dto.LoginRequest
import com.example.restapi.dto.SignupRequest
import com.example.restapi.dto.TokenRefreshResponse
import com.example.restapi.dto.UserDto
import io.vertx.core.Future

interface AuthService {
  fun signup(request: SignupRequest): Future<AuthResponse>

  fun login(request: LoginRequest): Future<AuthResponse>

  fun refreshToken(refreshTokenStr: String): Future<TokenRefreshResponse>

  fun getCurrentUser(userId: String): Future<UserDto>
}
