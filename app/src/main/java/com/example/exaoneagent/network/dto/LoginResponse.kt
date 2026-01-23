package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 로그인 응답 DTO
 *
 * 백엔드 API: POST /api/v1/auth/login
 */
data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("user")
    val user: UserInfo
) {
    data class UserInfo(
        @SerializedName("id")
        val id: Int,

        @SerializedName("email")
        val email: String,

        @SerializedName("name")
        val name: String,

        @SerializedName("employee_id")
        val employeeId: String,

        @SerializedName("dept_name")
        val deptName: String,

        @SerializedName("position")
        val position: String,

        @SerializedName("is_active")
        val isActive: Boolean
    )
}
