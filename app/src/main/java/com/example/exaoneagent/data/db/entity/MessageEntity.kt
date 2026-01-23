package com.example.exaoneagent.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: Long,
    val threadId: Long,
    val role: String,  // "user" or "assistant"
    val message: String,
    val correctedMsg: String? = null,
    val genSql: String? = null,
    val resultData: String? = null,  // JSON string
    val contextTag: String? = null,
    val createdAt: String
)
