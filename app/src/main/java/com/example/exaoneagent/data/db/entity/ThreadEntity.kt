package com.example.exaoneagent.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_threads")
data class ThreadEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val messageCount: Int,
    val createdAt: String,
    val updatedAt: String?
)
