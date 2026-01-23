package com.example.exaoneagent.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<String>>(emptyList())
    val messages: LiveData<List<String>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun sendMessage(message: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // 현재 메시지 목록에 새 메시지 추가
                val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
                currentMessages.add(message)
                _messages.value = currentMessages

                // 시뮬레이션: AI 응답 처리 (실제로는 API 호출)
                delay(1000)

                // 응답 메시지 추가
                currentMessages.add("AI 응답: $message")
                _messages.value = currentMessages

                _isLoading.value = false

            } catch (e: Exception) {
                _isLoading.value = false
                // 에러 처리
            }
        }
    }
}
