package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName

data class ChatMessageResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("thread_id")
    val threadId: Long,

    @SerializedName("role")
    val role: String,  // "user" or "assistant"

    @SerializedName("message")
    val message: String,

    @SerializedName("corrected_msg")
    val correctedMsg: String? = null,

    @SerializedName("gen_sql")
    val genSql: String? = null,

    @SerializedName("result_data")
    val resultData: QueryResultData? = null,

    @SerializedName("context_tag")
    val contextTag: String? = null,

    @SerializedName("created_at")
    val createdAt: String
)

data class QueryResultData(
    @SerializedName("columns")
    val columns: List<String>,

    @SerializedName("rows")
    val rows: List<Map<String, Any>>,

    @SerializedName("row_count")
    val rowCount: Int
)
