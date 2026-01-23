package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 로그인 요청 DTO
 *
 * 백엔드 API: POST /api/v1/auth/login
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)
