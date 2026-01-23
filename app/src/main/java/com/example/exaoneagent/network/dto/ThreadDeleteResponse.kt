package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 스레드 삭제 응답 DTO
 */
data class ThreadDeleteResponse(
    @SerializedName("thread_id")
    val threadId: Long,

    @SerializedName("deleted_messages_count")
    val deletedMessagesCount: Int,

    @SerializedName("deleted_at")
    val deletedAt: String
)
