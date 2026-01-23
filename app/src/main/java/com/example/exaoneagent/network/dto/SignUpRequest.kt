package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 회원가입 요청 DTO
 *
 * 백엔드 API: POST /api/v1/auth/signup
 */
data class SignUpRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("employee_id")
    val employeeId: String,

    @SerializedName("dept_name")
    val deptName: String,

    @SerializedName("position")
    val position: String
)
