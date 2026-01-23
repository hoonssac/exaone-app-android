package com.example.exaoneagent.network.dto

import com.google.gson.annotations.SerializedName

data class QueryRequest(
    @SerializedName("message")
    val message: String,

    @SerializedName("context_tag")
    val contextTag: String? = null,

    @SerializedName("thread_id")
    val threadId: Long? = null
)
