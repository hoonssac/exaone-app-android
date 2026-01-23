package com.example.exaoneagent.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.exaoneagent.data.db.entity.MessageEntity

@Dao
interface MessageDao {

    /**
     * 특정 스레드의 모든 메시지 조회 (시간순)
     */
    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY createdAt ASC")
    fun getMessagesByThread(threadId: Long): LiveData<List<MessageEntity>>

    /**
     * 특정 메시지 조회
     */
    @Query("SELECT * FROM chat_messages WHERE id = :messageId")
    fun getMessageById(messageId: Long): MessageEntity?

    /**
     * 메시지 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: MessageEntity)

    /**
     * 여러 메시지 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<MessageEntity>)

    /**
     * 메시지 업데이트
     */
    @Update
    fun updateMessage(message: MessageEntity)

    /**
     * 메시지 삭제
     */
    @Delete
    fun deleteMessage(message: MessageEntity)

    /**
     * ID로 메시지 삭제
     */
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    fun deleteMessageById(messageId: Long)

    /**
     * 특정 스레드의 모든 메시지 삭제
     */
    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    fun deleteMessagesByThread(threadId: Long)

    /**
     * 모든 메시지 삭제
     */
    @Query("DELETE FROM chat_messages")
    fun deleteAllMessages()
}
