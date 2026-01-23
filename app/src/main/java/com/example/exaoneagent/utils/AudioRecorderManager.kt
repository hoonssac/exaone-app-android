package com.example.exaoneagent.utils

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 음성 녹음 관리 클래스
 */
class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val tag = "AudioRecorderManager"

    /**
     * 음성 녹음 시작
     */
    fun startRecording(): Boolean {
        return try {
            // 오디오 파일 경로 설정
            val fileName = "audio_${System.currentTimeMillis()}.wav"
            audioFile = File(context.cacheDir, fileName)

            // MediaRecorder 초기화
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)  // WAV 대신 3GP 사용 (호환성)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            Log.d(tag, "✅ 음성 녹음 시작: ${audioFile?.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(tag, "❌ 음성 녹음 시작 실패: ${e.message}")
            false
        }
    }

    /**
     * 음성 녹음 중지
     */
    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            if (audioFile?.exists() == true && audioFile?.length()!! > 0) {
                val fileSize = audioFile?.length() ?: 0
                val filePath = audioFile?.absolutePath ?: "unknown"
                Log.d(tag, "✅ 음성 녹음 완료")
                Log.d(tag, "  파일: $filePath")
                Log.d(tag, "  크기: $fileSize bytes (${fileSize / 1024}KB)")
                Log.d(tag, "  형식: 3GP")
                audioFile
            } else {
                Log.e(tag, "❌ 녹음된 파일이 없거나 비어있음")
                Log.e(tag, "  파일 존재: ${audioFile?.exists()}")
                Log.e(tag, "  파일 크기: ${audioFile?.length()} bytes")
                audioFile?.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ 음성 녹음 중지 실패: ${e.message}")
            mediaRecorder?.release()
            mediaRecorder = null
            null
        }
    }

    /**
     * 음성 녹음 취소
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            audioFile?.delete()
            Log.d(tag, "🗑️ 음성 녹음 취소됨")
        } catch (e: Exception) {
            Log.e(tag, "❌ 음성 녹음 취소 실패: ${e.message}")
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    /**
     * 녹음 중 여부 확인
     */
    fun isRecording(): Boolean {
        return mediaRecorder != null
    }

    /**
     * 리소스 정리
     */
    fun release() {
        try {
            mediaRecorder?.apply {
                if (isRecording()) {
                    stop()
                }
                release()
            }
            mediaRecorder = null
            audioFile?.delete()
            Log.d(tag, "✅ AudioRecorderManager 리소스 정리 완료")
        } catch (e: Exception) {
            Log.e(tag, "❌ 리소스 정리 실패: ${e.message}")
        }
    }
}
