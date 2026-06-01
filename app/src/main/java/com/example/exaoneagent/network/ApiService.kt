package com.example.exaoneagent.network

import com.example.exaoneagent.network.dto.*
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody

interface ApiService {

    /**
     * 회원가입 API
     */
    @POST("api/v1/auth/signup")
    suspend fun signup(
        @Body request: SignUpRequest
    ): SignUpResponse

    /**
     * 로그인 API
     */
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    /**
     * 쿼리 처리 API (메시지 전송)
     * 새 스레드 생성 또는 기존 스레드에 메시지 추가
     */
    @POST("api/v1/query/")
    suspend fun processQuery(
        @Body request: QueryRequest,
        @Header("Authorization") token: String
    ): QueryResponse

    /**
     * 사용자의 모든 스레드 조회
     */
    @GET("api/v1/query/threads")
    suspend fun getThreads(
        @Header("Authorization") token: String
    ): List<ChatThreadResponse>

    /**
     * 특정 스레드의 메시지 조회
     */
    @GET("api/v1/query/threads/{thread_id}/messages")
    suspend fun getThreadMessages(
        @Path("thread_id") threadId: Long,
        @Header("Authorization") token: String
    ): List<ChatMessageResponse>

    /**
     * 음성 파일 업로드 및 STT 처리 API
     * 음성 파일을 텍스트로 변환 후 쿼리 처리
     */
    @Multipart
    @POST("api/v1/query/voice")
    suspend fun processVoiceQuery(
        @Part file: MultipartBody.Part,
        @Part("thread_id") threadId: RequestBody?,
        @Header("Authorization") token: String
    ): QueryResponse

    /**
     * TTS (Text-to-Speech) API
     * 텍스트를 음성 파일로 변환
     */
    @POST("api/v1/query/tts")
    suspend fun convertTextToSpeech(
        @Body request: TTSRequest,
        @Header("Authorization") token: String
    ): ResponseBody

    /**
     * 스레드 삭제 API
     * 스레드와 메시지를 soft delete
     */
    @DELETE("api/v1/query/threads/{thread_id}")
    suspend fun deleteThread(
        @Path("thread_id") threadId: Long,
        @Header("Authorization") token: String
    ): ThreadDeleteResponse

}
