package com.example.exaoneagent.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.exaoneagent.data.db.AppDatabase
import com.example.exaoneagent.data.db.entity.MessageEntity
import com.example.exaoneagent.data.db.entity.ThreadEntity
import com.example.exaoneagent.data.local.PreferenceManager
import com.example.exaoneagent.data.repository.ChatRepository
import com.example.exaoneagent.network.RetrofitClient
import com.example.exaoneagent.network.dto.QueryResponse
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val preferenceManager = PreferenceManager(application)
    private val repository = ChatRepository(
        RetrofitClient.apiService,
        database,
        preferenceManager
    )

    private val tag = "ChatViewModel"

    // TTS 변환 이벤트
    private val _ttsEvent = MutableLiveData<ByteArray?>()
    val ttsEvent: LiveData<ByteArray?> get() = _ttsEvent

    // 모든 스레드
    private val _allThreads: LiveData<List<ThreadEntity>> = repository.getAllThreads()
    val allThreads: LiveData<List<ThreadEntity>> = _allThreads

    // 현재 선택된 스레드
    private val _currentThread = MutableLiveData<ThreadEntity>()
    val currentThread: LiveData<ThreadEntity> = _currentThread

    // 현재 스레드의 메시지들
    private val _currentMessages = MutableLiveData<LiveData<List<MessageEntity>>>()
    val currentMessages = _currentMessages

    // 로딩 상태
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 에러 메시지
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 쿼리 응답
    private val _queryResponse = MutableLiveData<QueryResponse>()
    val queryResponse: LiveData<QueryResponse> = _queryResponse

    init {
        Log.d(tag, "✅ ChatViewModel 초기화")
    }

    /**
     * 서버에서 스레드 목록 동기화
     */
    fun syncThreadsFromServer() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.fetchThreadsFromServer()
                Log.d(tag, "✅ 스레드 동기화 완료")
            } catch (e: Exception) {
                _errorMessage.value = "스레드 동기화 실패: ${e.message}"
                Log.e(tag, "❌ 스레드 동기화 실패: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 특정 스레드 선택
     */
    fun selectThread(thread: ThreadEntity) {
        _currentThread.value = thread
        // 1️⃣ 먼저 로컬 DB에서 메시지 로드 (즉시 표시)
        _currentMessages.value = repository.getThreadMessages(thread.id)

        // 2️⃣ 백그라운드에서 서버에서 메시지 동기화 (최신 메시지 받아오기)
        viewModelScope.launch {
            try {
                repository.fetchThreadMessagesFromServer(thread.id)
                // 동기화 후 메시지 다시 로드
                _currentMessages.value = repository.getThreadMessages(thread.id)
                Log.d(tag, "✅ 서버에서 메시지 동기화 완료: ${thread.id}")
            } catch (e: Exception) {
                Log.e(tag, "❌ 메시지 동기화 실패: ${e.message}")
            }
        }

        Log.d(tag, "✅ 스레드 선택: ${thread.id}")
    }

    /**
     * 새 채팅 시작 (null 상태 설정)
     */
    fun startNewChat() {
        _currentThread.value = null
        _currentMessages.value = null
        Log.d(tag, "✅ 새 채팅 시작")
    }

    /**
     * 메시지 전송
     */
    fun sendMessage(
        message: String,
        contextTag: String? = null
    ) {
        if (message.trim().isEmpty()) {
            _errorMessage.value = "메시지를 입력해주세요"
            return
        }

        val threadId = _currentThread.value?.id

        // 백그라운드에서 서버 요청
        viewModelScope.launch {
            // 로딩 지연 표시를 위한 Job
            val loadingJob = launch {
                kotlinx.coroutines.delay(600)
                _isLoading.value = true
            }

            try {
                // 사용자 메시지 저장 및 API 호출 (Repository 내부에서 순차 처리)
                // Repository에서 사용자 메시지를 먼저 저장하므로 UI에는 바로 뜸
                val response = repository.sendMessage(
                    message = message,
                    contextTag = contextTag,
                    threadId = threadId
                )

                if (response != null) {
                    _queryResponse.value = response

                    // 새 스레드 생성된 경우
                    if (threadId == null) {
                        val newThread = ThreadEntity(
                            id = response.threadId,
                            title = message.take(50),
                            messageCount = 2,
                            createdAt = response.createdAt,
                            updatedAt = response.createdAt
                        )

                        // 새 스레드: 메시지는 sendMessage()에서 이미 저장했으므로 바로 표시
                        _currentThread.value = newThread
                        _currentMessages.value = repository.getThreadMessages(newThread.id)
                        Log.d(tag, "✅ 새 스레드 생성 및 메시지 표시")
                    } else {
                        // 기존 스레드: selectThread 호출해서 서버에서 메시지 동기화
                        _currentThread.value?.let { selectThread(it) }
                        Log.d(tag, "✅ 기존 스레드에 메시지 추가")
                    }

                    Log.d(tag, "✅ 메시지 전송 완료")
                } else {
                    _errorMessage.value = "메시지 전송 실패"
                    Log.e(tag, "❌ 메시지 전송 실패")
                }
            } finally {
                loadingJob.cancel() // 응답이 오면 로딩 켜지는 예약 취소
                _isLoading.value = false
            }
        }
    }

    /**
     * 새 스레드 시작
     */
    fun startNewThread() {
        _currentThread.value = null
        _currentMessages.value = null
        _queryResponse.value = null
        Log.d(tag, "✅ 새 스레드 시작")
    }

    /**
     * 스레드 삭제
     */
    fun deleteThread(threadId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteThread(threadId)

                // 현재 선택된 스레드가 삭제된 것이면 새 스레드 시작
                if (_currentThread.value?.id == threadId) {
                    startNewThread()
                }

                Log.d(tag, "✅ 스레드 $threadId 삭제 완료")
            } catch (e: Exception) {
                _errorMessage.value = "스레드 삭제 실패: ${e.message}"
                Log.e(tag, "❌ 스레드 삭제 실패: ${e.message}")
            }
        }
    }

    /**
     * 모든 데이터 삭제
     */
    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                startNewThread()
                Log.d(tag, "✅ 모든 데이터 삭제 완료")
            } catch (e: Exception) {
                _errorMessage.value = "데이터 삭제 실패: ${e.message}"
                Log.e(tag, "❌ 데이터 삭제 실패: ${e.message}")
            }
        }
    }

    /**
     * 음성 메시지 전송
     */
    fun sendVoiceMessage(audioFile: File) {
        val threadId = _currentThread.value?.id

        // 백그라운드에서 서버 요청
        viewModelScope.launch {
            // 로딩 지연 표시를 위한 Job
            val loadingJob = launch {
                kotlinx.coroutines.delay(600)
                _isLoading.value = true
            }

            try {
                val response = repository.sendVoiceMessage(
                    audioFile = audioFile,
                    threadId = threadId
                )

                if (response != null) {
                    _queryResponse.value = response

                    // 새 스레드 생성된 경우
                    if (threadId == null) {
                        val newThread = ThreadEntity(
                            id = response.threadId,
                            title = "음성 메시지",
                            messageCount = 2,
                            createdAt = response.createdAt,
                            updatedAt = response.createdAt
                        )

                        // 새 스레드: 메시지는 sendVoiceMessage()에서 이미 저장했으므로 바로 표시
                        _currentThread.value = newThread
                        _currentMessages.value = repository.getThreadMessages(newThread.id)
                        Log.d(tag, "✅ 새 스레드 생성 및 메시지 표시")
                    } else {
                        // 기존 스레드: selectThread 호출해서 서버에서 메시지 동기화
                        _currentThread.value?.let { selectThread(it) }
                        Log.d(tag, "✅ 기존 스레드에 메시지 추가")
                    }

                    Log.d(tag, "✅ 음성 메시지 전송 완료")
                } else {
                    Log.e(tag, "❌ 음성 메시지 전송 실패")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ 음성 메시지 전송 중 오류: ${e.message}")
            } finally {
                loadingJob.cancel()
                _isLoading.value = false
                // 녹음 파일 삭제
                audioFile.delete()
                Log.d(tag, "🗑️ 녹음 파일 삭제됨")
            }
        }
    }

    /**
     * 텍스트를 음성으로 변환 (TTS)
     */
    fun convertToSpeech(text: String) {
        viewModelScope.launch {
            try {
                Log.d(tag, "🔊 TTS 변환 시작")
                val audioBytes = repository.convertTextToSpeech(text)

                if (audioBytes != null) {
                    _ttsEvent.value = audioBytes
                    Log.d(tag, "✅ TTS 변환 완료, 재생 준비")
                } else {
                    _errorMessage.value = "TTS 변환 실패"
                    Log.e(tag, "❌ TTS 변환 실패")
                }
            } catch (e: Exception) {
                _errorMessage.value = "TTS 변환 중 오류: ${e.message}"
                Log.e(tag, "❌ TTS 변환 중 오류: ${e.message}")
            }
        }
    }
}
