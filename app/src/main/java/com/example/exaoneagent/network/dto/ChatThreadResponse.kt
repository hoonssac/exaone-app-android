package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class ChatThreadResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("message_count")
    val messageCount: Int,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String?
)
