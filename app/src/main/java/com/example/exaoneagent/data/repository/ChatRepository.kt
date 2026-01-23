package com.example.exaoneagent.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.exaoneagent.data.db.AppDatabase
import com.example.exaoneagent.data.db.entity.MessageEntity
import com.example.exaoneagent.data.db.entity.ThreadEntity
import com.example.exaoneagent.data.local.PreferenceManager
import com.example.exaoneagent.network.ApiService
import com.example.exaoneagent.network.dto.QueryRequest
import com.example.exaoneagent.network.dto.QueryResponse
import com.example.exaoneagent.network.dto.ChatThreadResponse
import com.example.exaoneagent.network.dto.ChatMessageResponse
import com.example.exaoneagent.network.dto.TTSRequest
import com.example.exaoneagent.network.dto.ThreadDeleteResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatRepository(
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val preferenceManager: PreferenceManager
) {

    private val threadDao = database.threadDao()
    private val messageDao = database.messageDao()
    private val tag = "ChatRepository"

    /**
     * 모든 스레드 조회 (로컬 캐시)
     */
    fun getAllThreads(): LiveData<List<ThreadEntity>> {
        return threadDao.getAllThreads()
    }

    /**
     * 서버에서 모든 스레드 조회 및 로컬 저장
     */
    suspend fun fetchThreadsFromServer() {
        try {
            val token = preferenceManager.getAccessToken() ?: return
            val threads = apiService.getThreads("Bearer $token")

            // ThreadEntity로 변환
            val threadEntities = threads.map { thread ->
                ThreadEntity(
                    id = thread.id,
                    title = thread.title,
                    messageCount = thread.messageCount,
                    createdAt = thread.createdAt,
                    updatedAt = thread.updatedAt
                )
            }

            // 로컬 DB에 저장
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                threadDao.insertThreads(threadEntities)
            }
            Log.d(tag, "✅ ${threadEntities.size}개의 스레드 동기화 완료")
        } catch (e: Exception) {
            Log.e(tag, "❌ 스레드 조회 실패: ${e.message}")
        }
    }

    /**
     * 특정 스레드의 메시지 조회 (로컬 캐시)
     */
    fun getThreadMessages(threadId: Long): LiveData<List<MessageEntity>> {
        return messageDao.getMessagesByThread(threadId)
    }

    /**
     * 서버에서 특정 스레드의 메시지 조회 및 로컬 저장
     */
    suspend fun fetchThreadMessagesFromServer(threadId: Long) {
        try {
            val token = preferenceManager.getAccessToken() ?: return
            val messages = apiService.getThreadMessages(threadId, "Bearer $token")

            // MessageEntity로 변환
            val messageEntities = messages.map { message ->
                MessageEntity(
                    id = message.id,
                    threadId = message.threadId,
                    role = message.role,
                    message = message.message,
                    correctedMsg = message.correctedMsg,
                    genSql = message.genSql,
                    resultData = message.resultData?.let {
                        com.google.gson.Gson().toJson(it)
                    },
                    contextTag = message.contextTag,
                    createdAt = message.createdAt
                )
            }

            // 로컬 DB에 저장 (기존 메시지 삭제 후 새 메시지 저장)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                messageDao.deleteMessagesByThread(threadId)
                messageDao.insertMessages(messageEntities)
            }
            Log.d(tag, "✅ 스레드 $threadId - ${messageEntities.size}개 메시지 동기화 완료")
        } catch (e: Exception) {
            Log.e(tag, "❌ 메시지 조회 실패: ${e.message}")
        }
    }

    /**
     * 새로운 메시지 전송
     */
    suspend fun sendMessage(
        message: String,
        contextTag: String? = null,
        threadId: Long? = null
    ): QueryResponse? {
        return try {
            val token = preferenceManager.getAccessToken() ?: return null

            // 1. 임시 사용자 메시지 선 저장 (UI 즉시 반영용)
            // threadId가 없으면 임시 스레드 ID 생성 (음수 사용 등) 또는 0 처리
            // 여기서는 간단히 threadId가 있을 때만 선 저장하거나, 새 채팅일 경우 처리가 복잡해지므로
            // 새 채팅일 때도 동작하게 하려면 로컬에서 먼저 스레드를 만들어야 함.
            // 우선 threadId가 있는 경우(기존 채팅)에만 선 반영을 적용해 봅니다.
            var tempMessageId: Long? = null
            
            if (threadId != null) {
                tempMessageId = System.currentTimeMillis()
                val tempUserMessage = MessageEntity(
                    id = tempMessageId, 
                    threadId = threadId,
                    role = "user",
                    message = message,
                    contextTag = contextTag,
                    createdAt = java.time.LocalDateTime.now().toString() // ISO 형식 필요
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    messageDao.insertMessage(tempUserMessage)
                }
            }

            // 2. 서버에 요청
            val request = QueryRequest(
                message = message,
                contextTag = contextTag,
                threadId = threadId
            )
            val response = apiService.processQuery(request, "Bearer $token")

            // 3. 응답 처리
            val actualThreadId = response.threadId

            // 새 채팅이었으면 스레드 생성
            val existingThread = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                threadDao.getThreadById(actualThreadId)
            }
            if (existingThread == null) {
                val newThread = ThreadEntity(
                    id = actualThreadId,
                    title = message.take(50),
                    messageCount = 0,
                    createdAt = response.createdAt,
                    updatedAt = response.createdAt
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    threadDao.insertThread(newThread)
                }
            }

            // 4. 임시 메시지 삭제 및 실제 메시지 저장
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (tempMessageId != null) {
                    messageDao.deleteMessageById(tempMessageId) // 임시 삭제
                }
                
                // 실제 사용자 메시지 저장 (서버 ID 사용)
                val userMessage = MessageEntity(
                    id = response.messageId,
                    threadId = actualThreadId,
                    role = "user",
                    message = message,
                    contextTag = contextTag,
                    createdAt = response.createdAt
                )
                messageDao.insertMessage(userMessage)
            }
            Log.d(tag, "✅ 사용자 메시지 저장됨 (스레드: $actualThreadId)")

            // 5. AI 응답 메시지 저장
            saveAIMessage(response)
            Log.d(tag, "✅ AI 응답 메시지 저장됨 (스레드: ${response.threadId})")

            response
        } catch (e: Exception) {
            Log.e(tag, "❌ 메시지 전송 실패: ${e.message}")
            null
        }
    }

    /**
     * AI 응답 메시지만 저장
     */
    private suspend fun saveAIMessage(response: QueryResponse) {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // AI 응답 메시지 저장 (백엔드에서 생성한 자연어 응답 사용)
                val assistantMessage = MessageEntity(
                    id = response.messageId + 1,  // 임시 ID (서버에서 받지 않음)
                    threadId = response.threadId,
                    role = "assistant",
                    message = response.naturalResponse,  // 실제 ChatGPT 생성 응답
                    correctedMsg = response.correctedMessage,
                    genSql = response.generatedSql,
                    resultData = com.google.gson.Gson().toJson(response.resultData),
                    createdAt = response.createdAt
                )
                messageDao.insertMessage(assistantMessage)
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ AI 메시지 저장 실패: ${e.message}")
        }
    }

    /**
     * 스레드 삭제 (백엔드와 동기화)
     */
    suspend fun deleteThread(threadId: Long) {
        try {
            val token = preferenceManager.getAccessToken() ?: run {
                Log.e(tag, "❌ 토큰이 없습니다")
                return
            }

            // 백엔드 API 호출
            val response = apiService.deleteThread(threadId, "Bearer $token")
            Log.d(tag, "📤 스레드 삭제 요청: $threadId")

            // 로컬 DB에서 스레드와 메시지 삭제
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val thread = threadDao.getThreadById(threadId) ?: return@withContext
                threadDao.deleteThread(thread)
                messageDao.deleteMessagesByThread(threadId)
            }

            Log.d(tag, "✅ 스레드 $threadId 삭제 완료")
            Log.d(tag, "   - 삭제된 메시지: ${response.deletedMessagesCount}개")
            Log.d(tag, "   - 삭제 시간: ${response.deletedAt}")
        } catch (e: Exception) {
            Log.e(tag, "❌ 스레드 삭제 실패: ${e.message}")
            throw e
        }
    }

    /**
     * 모든 로컬 데이터 삭제
     */
    suspend fun clearAllData() {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                threadDao.deleteAllThreads()
                messageDao.deleteAllMessages()
            }
            Log.d(tag, "✅ 모든 로컬 데이터 삭제 완료")
        } catch (e: Exception) {
            Log.e(tag, "❌ 데이터 삭제 실패: ${e.message}")
        }
    }

    /**
     * 음성 파일 업로드 및 STT 처리
     */
    suspend fun sendVoiceMessage(
        audioFile: File,
        threadId: Long? = null
    ): QueryResponse? {
        return try {
            val token = preferenceManager.getAccessToken()
            Log.d(tag, "🔐 토큰 확인: ${if (token != null) "존재" else "없음"}")
            if (token == null) {
                Log.e(tag, "❌ 토큰이 없습니다. 다시 로그인해주세요")
                return null
            }

            // MultipartBody.Part로 파일 생성 (백엔드에서는 "file" 필드명 기대)
            val requestBody = audioFile.asRequestBody()
            val filePart = MultipartBody.Part.createFormData(
                "file",
                audioFile.name,
                requestBody
            )

            Log.d(tag, "📤 음성 파일 업로드: ${audioFile.name} (${audioFile.length()}bytes), threadId: $threadId")
            // API 호출 (threadId 함께 전송)
            val response = apiService.processVoiceQuery(filePart, threadId, "Bearer $token")

            // 응답 처리 (텍스트 쿼리와 동일한 방식)
            val actualThreadId = response.threadId

            // 1. 스레드 생성 (필요하면)
            val existingThread = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                threadDao.getThreadById(actualThreadId)
            }
            if (existingThread == null) {
                val newThread = ThreadEntity(
                    id = actualThreadId,
                    title = "음성 메시지",
                    messageCount = 0,
                    createdAt = response.createdAt,
                    updatedAt = response.createdAt
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    threadDao.insertThread(newThread)
                }
            }

            // 2. 사용자 메시지 저장 (음성 → 텍스트 변환된 내용)
            val userMessage = MessageEntity(
                id = response.messageId,
                threadId = actualThreadId,
                role = "user",
                message = "[음성 메시지]",  // 변환된 텍스트는 서버에서 처리
                contextTag = null,
                createdAt = response.createdAt
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                messageDao.insertMessage(userMessage)
            }
            Log.d(tag, "✅ 사용자 음성 메시지 저장됨 (스레드: $actualThreadId)")

            // 3. AI 응답 메시지 저장
            saveAIMessage(response)
            Log.d(tag, "✅ AI 응답 메시지 저장됨 (스레드: ${response.threadId})")

            response
        } catch (e: Exception) {
            Log.e(tag, "❌ 음성 메시지 전송 실패: ${e.message}")
            null
        }
    }

    /**
     * 텍스트를 음성 파일로 변환 (TTS)
     */
    suspend fun convertTextToSpeech(
        text: String,
        speaker: String = "M1"
    ): ByteArray? {
        return try {
            val token = preferenceManager.getAccessToken() ?: return null

            val request = TTSRequest(
                text = text,
                language = "ko",
                speaker = speaker
            )

            Log.d(tag, "🔊 TTS 변환 시작: $text (speaker: $speaker)")

            val response = apiService.convertTextToSpeech(request, "Bearer $token")
            val audioBytes = response.bytes()

            Log.d(tag, "✅ TTS 변환 완료: ${audioBytes.size} bytes")
            audioBytes
        } catch (e: Exception) {
            Log.e(tag, "❌ TTS 변환 실패: ${e.message}")
            null
        }
    }
}
