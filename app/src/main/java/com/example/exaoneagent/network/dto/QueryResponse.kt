package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName

data class QueryResponse(
    @SerializedName("thread_id")
    val threadId: Long,

    @SerializedName("message_id")
    val messageId: Long,

    @SerializedName("original_message")
    val originalMessage: String,

    @SerializedName("corrected_message")
    val correctedMessage: String,

    @SerializedName("generated_sql")
    val generatedSql: String,

    @SerializedName("result_data")
    val resultData: QueryResultData,

    @SerializedName("execution_time")
    val executionTime: Double,

    @SerializedName("natural_response")
    val naturalResponse: String,

    @SerializedName("created_at")
    val createdAt: String
)
