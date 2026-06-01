package com.example.exaoneagent.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.media.MediaPlayer
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exaoneagent.databinding.FragmentChatBinding
import com.example.exaoneagent.data.db.entity.MessageEntity
import com.example.exaoneagent.ui.chat.adapter.MessageAdapter
import com.example.exaoneagent.utils.AudioRecorderManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var messageAdapter: MessageAdapter
    private val tag = "ChatFragment"
    private var lastMessageCount = 0
    private var currentMessageList: List<MessageEntity> = emptyList()

    // 음성 녹음 관련
    private lateinit var audioRecorderManager: AudioRecorderManager
    private var isRecording = false
    private val PERMISSION_REQUEST_CODE = 100

    // 알림 카드 클릭 후 음성 메시지를 "메일로 보내줘"로 고정
    private var isAfterNotificationCard = false

    // 음성 재생 관련
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        audioRecorderManager = AudioRecorderManager(requireContext())

        setupRecyclerView()
        setupObservers()
        setupListeners()

        Log.d(tag, "✅ ChatFragment 초기화 완료")
    }

    /**
     * ViewModel 반환 (MainActivity에서 접근용)
     */
    fun getChatViewModel(): ChatViewModel {
        return viewModel
    }

    /**
     * RecyclerView 설정
     */
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.rvMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true  // 메시지가 아래에서 위로 쌓임
            }
        }

        // 어댑터 데이터 변경 감지
        messageAdapter.registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                // 새 아이템이 삽입되면 맨 아래로 스크롤
                binding.rvMessages.post {
                    val lastPosition = messageAdapter.itemCount - 1
                    if (lastPosition >= 0) {
                        binding.rvMessages.scrollToPosition(lastPosition)
                        Log.d(tag, "어댑터 데이터 변경: $lastPosition 위치로 스크롤")
                    }
                }
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                // 아이템이 변경되면 맨 아래로 스크롤 (로딩 -> 메시지 변경 시)
                binding.rvMessages.post {
                    val lastPosition = messageAdapter.itemCount - 1
                    if (lastPosition >= 0) {
                        binding.rvMessages.scrollToPosition(lastPosition)
                        Log.d(tag, "어댑터 아이템 변경: $lastPosition 위치로 스크롤")
                    }
                }
            }
        })
    }

    /**
     * ViewModel 옵저버 설정
     */
    private fun setupObservers() {
        // 스레드 변경 감지하여 어댑터 상태 초기화
        viewModel.currentThread.observe(viewLifecycleOwner) { thread ->
            messageAdapter.resetAnimationState()
        }

        // switchMap으로 nested LiveData 문제 해결
        val messages: LiveData<List<MessageEntity>> = viewModel.currentMessages.switchMap { messagesLiveData ->
            if (messagesLiveData != null) {
                messagesLiveData
            } else {
                // 새 채팅: 빈 리스트 반환
                androidx.lifecycle.MutableLiveData(emptyList<MessageEntity>())
            }
        }

        messages.observe(viewLifecycleOwner) { messageList ->
            try {
                currentMessageList = messageList
                updateAdapterList(currentMessageList, viewModel.isLoading.value == true)
                
                lastMessageCount = messageList.size
                Log.d(tag, "✅ 메시지 로드: ${messageList.size}개")
            } catch (e: Exception) {
                Log.e(tag, "❌ 메시지 로드 실패: ${e.message}")
            }
        }

        // 로딩 상태 변경 감지
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSend.isEnabled = !isLoading
            // 녹음 중이 아닐 때만 음성 버튼 제어 (녹음 중엔 멈추기 위해 활성 유지)
            if (!isRecording) {
                binding.btnVoice.isEnabled = !isLoading
            }
            updateAdapterList(currentMessageList, isLoading)
        }

        // 에러 메시지
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                com.google.android.material.snackbar.Snackbar.make(binding.root, error, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            }
        }

        // 쿼리 응답 - 타이핑 애니메이션 + TTS 변환
        viewModel.queryResponse.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                Log.d(tag, "✅ 응답 받음 - 스레드: ${response.threadId}, 메시지: ${response.messageId}")

                viewModel.startTypewriter(response.naturalResponse)

                // 첫 문장 + 마지막 문장만 TTS로 읽기
                val ttsText = extractFirstAndLastSentence(response.naturalResponse)
                Log.d(tag, "🔊 TTS 텍스트: $ttsText")
                viewModel.convertToSpeech(ttsText)
            }
        }

        // 타이핑 애니메이션: 전체 텍스트가 일치하는 ViewHolder만 업데이트 (이전 메시지 덮어쓰기 방지)
        viewModel.typingState.observe(viewLifecycleOwner) { state ->
            if (state != null) {
                val (fullText, partialText) = state
                val targetPos = messageAdapter.currentList.indexOfLast {
                    it.role == "assistant" && it.message == fullText
                }
                if (targetPos >= 0) {
                    val vh = binding.rvMessages.findViewHolderForAdapterPosition(targetPos)
                    (vh as? MessageAdapter.AssistantMessageViewHolder)?.updateText(partialText)
                }
            }
        }

        // TTS 오디오 재생
        viewModel.ttsEvent.observe(viewLifecycleOwner) { audioBytes ->
            if (audioBytes != null) {
                playAudio(audioBytes)
            } else {
                com.google.android.material.snackbar.Snackbar.make(binding.root, "음성 재생 실패", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAdapterList(messages: List<MessageEntity>, isLoading: Boolean) {
        val finalList = messages.toMutableList()
        if (isLoading) {
            finalList.add(MessageEntity(
                id = -9999L,
                threadId = -1,
                role = "loading",
                message = "",
                createdAt = ""
            ))
        }
        messageAdapter.submitList(finalList)
        updateGuideVisibility(messages)
    }

    /**
     * 버튼 리스너 설정
     */
    private fun setupListeners() {
        // 전송 버튼
        binding.btnSend.setOnClickListener {
            val message = binding.etInput.text.toString().trim()
            if (message.isEmpty()) {
                com.google.android.material.snackbar.Snackbar.make(binding.root, "메시지를 입력해주세요", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.sendMessage(
                message = message,
                contextTag = "@일반"  // 기본값
            )

            binding.etInput.text.clear()
            Log.d(tag, "메시지 전송: $message")
        }

        // 음성 입력 버튼
        binding.btnVoice.setOnClickListener {
            handleVoiceButtonClick()
        }

        // 알림 카드 클릭 시 메시지 자동 전송
        binding.cardNotification.setOnClickListener {
            val autoMessage = "최근 일주일간 발생한 불량 내용을 알려줘"
            isAfterNotificationCard = true
            viewModel.sendMessage(
                message = autoMessage,
                contextTag = "@일반"
            )
            Log.d(tag, "알림 카드 클릭: 메시지 자동 전송 - $autoMessage")
        }
    }

    /**
     * 메시지 리스트 길이에 따라 가이드 표시/숨김
     */
    private fun updateGuideVisibility(messages: List<*>) {
        if (messages.isEmpty()) {
            // 메시지가 없으면 가이드 표시
            binding.guideContainer.visibility = View.VISIBLE
            binding.rvMessages.visibility = View.GONE
            
            // 알림 카드 애니메이션 (위에서 아래로 슬라이드 + 페이드 인)
            if (binding.cardNotification.visibility != View.VISIBLE) {
                binding.cardNotification.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    translationY = -50f
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .setStartDelay(500) // 타이틀이 보인 후 부드럽게 등장하도록 딜레이
                        .start()
                }
            }
        } else {
            // 메시지가 있으면 메시지 목록 표시
            binding.guideContainer.visibility = View.GONE
            binding.rvMessages.visibility = View.VISIBLE
            binding.cardNotification.visibility = View.GONE
        }
    }

    /**
     * 음성 버튼 클릭 처리
     */
    private fun handleVoiceButtonClick() {
        if (!isRecording) {
            // 녹음 시작
            if (checkMicrophonePermission()) {
                startVoiceRecording()
            } else {
                requestMicrophonePermission()
            }
        } else {
            // 녹음 중지
            stopVoiceRecording()
        }
    }

    /**
     * 마이크 권한 확인
     */
    private fun checkMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 마이크 권한 요청
     */
    private fun requestMicrophonePermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }

    /**
     * 음성 녹음 시작
     */
    private fun startVoiceRecording() {
        if (audioRecorderManager.startRecording()) {
            isRecording = true
            binding.btnVoice.setIconTintResource(android.R.color.holo_red_light)
            Log.d(tag, "🎤 음성 녹음 시작")
        } else {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "음성 녹음을 시작할 수 없습니다", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * 음성 녹음 중지 및 전송
     */
    private fun stopVoiceRecording() {
        val audioFile = audioRecorderManager.stopRecording()

        isRecording = false
        binding.btnVoice.setIconTintResource(android.R.color.darker_gray)

        if (audioFile != null && audioFile.exists()) {
            if (isAfterNotificationCard) {
                // 알림 카드 이후 음성은 "메일로 보내줘"로 고정 전송
                isAfterNotificationCard = false
                audioFile.delete()
                viewModel.sendMessage(
                    message = "메일로 보내줘",
                    contextTag = "@일반"
                )
                Log.d(tag, "✅ 알림 카드 후속 음성 → '메일로 보내줘' 텍스트로 전송")
            } else {
                viewModel.sendVoiceMessage(audioFile)
                Log.d(tag, "✅ 음성 메시지 전송: ${audioFile.absolutePath}")
            }
        } else {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "음성 녹음이 실패했습니다", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            Log.e(tag, "❌ 녹음된 파일이 없음")
        }
    }

    /**
     * 권한 요청 결과 처리
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording()
                Log.d(tag, "✅ 마이크 권한 승인됨")
            } else {
                com.google.android.material.snackbar.Snackbar.make(binding.root, "마이크 권한이 필요합니다", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                Log.d(tag, "❌ 마이크 권한 거부됨")
            }
        }
    }

    /**
     * 응답 텍스트에서 첫 문장과 마지막 문장만 추출
     * 예) "최근 일주일 기준 불량 현황 요약입니다. 주요 불량 유형: ... 필요하시다면 메일로 보내드릴까요?"
     *  → "최근 일주일 기준 불량 현황 요약입니다. 필요하시다면 메일로 보내드릴까요?"
     */
    private fun extractFirstAndLastSentence(text: String): String {
        // 마침표·물음표·느낌표 또는 줄바꿈 기준으로 문장 분리
        val sentences = text
            .split(Regex("(?<=[.?!])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return when {
            sentences.size <= 1 -> text
            else -> "${sentences.first()} ${sentences.last()}"
        }
    }

    /**
     * 바이트 배열을 음성으로 재생
     */
    private fun playAudio(audioBytes: ByteArray) {
        try {
            // 임시 파일에 음성 데이터 저장
            val audioFile = File(requireContext().cacheDir, "response_${System.currentTimeMillis()}.wav")
            val fos = FileOutputStream(audioFile)
            fos.write(audioBytes)
            fos.close()

            // MediaPlayer 초기화
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()

                setOnCompletionListener {
                    audioFile.delete()
                    Log.d(tag, "✅ 음성 재생 완료, 파일 삭제됨")
                }
            }

            Log.d(tag, "🔊 음성 재생 시작: ${audioFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(tag, "❌ 음성 재생 실패: ${e.message}")
            com.google.android.material.snackbar.Snackbar.make(binding.root, "음성 재생 실패", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        audioRecorderManager.release()
        _binding = null
    }
}