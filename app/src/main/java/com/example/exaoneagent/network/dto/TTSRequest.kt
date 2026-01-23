package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName

/**
 * TTS (Text-to-Speech) 요청 DTO
 */
data class TTSRequest(
    @SerializedName("text")
    val text: String,

    @SerializedName("language")
    val language: String = "ko",  // 기본값: 한국어

    @SerializedName("speaker")
    val speaker: String = "M1"  // 기본값: 남성 음성
)
