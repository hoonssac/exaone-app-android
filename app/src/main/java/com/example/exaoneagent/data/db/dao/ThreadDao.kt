package com.example.exaoneagent.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.exaoneagent.data.db.entity.ThreadEntity

@Dao
interface ThreadDao {

    /**
     * 모든 스레드 조회 (최신순: ID 역순)
     */
    @Query("SELECT * FROM chat_threads ORDER BY id DESC")
    fun getAllThreads(): LiveData<List<ThreadEntity>>

    /**
     * 특정 스레드 조회
     */
    @Query("SELECT * FROM chat_threads WHERE id = :threadId")
    fun getThreadById(threadId: Long): ThreadEntity?

    /**
     * 스레드 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertThread(thread: ThreadEntity)

    /**
     * 여러 스레드 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertThreads(threads: List<ThreadEntity>)

    /**
     * 스레드 업데이트
     */
    @Update
    fun updateThread(thread: ThreadEntity)

    /**
     * 스레드 삭제
     */
    @Delete
    fun deleteThread(thread: ThreadEntity)

    /**
     * 모든 스레드 삭제
     */
    @Query("DELETE FROM chat_threads")
    fun deleteAllThreads()
}
