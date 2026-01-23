package com.example.exaoneagent.ui.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.exaoneagent.network.RetrofitClient
import com.example.exaoneagent.network.dto.SignUpRequest
import com.example.exaoneagent.data.local.PreferenceManager
import android.content.Context
import android.util.Log

sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    data class Success(val message: String) : SignUpState()
    data class Error(val message: String) : SignUpState()
}

class SignUpViewModel(val context: Context) : ViewModel() {
    private val _signUpState = MutableLiveData<SignUpState>(SignUpState.Idle)
    val signUpState: LiveData<SignUpState> = _signUpState

    private val preferenceManager = PreferenceManager(context)

    fun signUp(email: String, name: String, password: String, employeeId: String, dept: String, position: String) {
        viewModelScope.launch {
            try {
                _signUpState.value = SignUpState.Loading

                // API 호출
                val request = SignUpRequest(
                    email = email,
                    password = password,
                    name = name,
                    employeeId = employeeId,
                    deptName = dept,
                    position = position
                )

                val response = RetrofitClient.apiService.signup(request)

                // 토큰 저장
                preferenceManager.saveAccessToken(response.accessToken)
                preferenceManager.saveRefreshToken(response.refreshToken)

                // 사용자 정보 저장
                preferenceManager.saveUserId(response.user.id)
                preferenceManager.saveUserEmail(response.user.email)
                preferenceManager.saveUserName(response.user.name)
                preferenceManager.saveDeptName(response.user.deptName)
                preferenceManager.savePosition(response.user.position)
                preferenceManager.saveLoginState(true)

                // 로그 출력 (확인용)
                Log.d("SignUpViewModel", "✅ 회원가입 성공!")
                Log.d("SignUpViewModel", "🔐 Access Token: ${response.accessToken.substring(0, 20)}...")
                Log.d("SignUpViewModel", "👤 사용자: ${response.user.email} (${response.user.name})")
                Log.d("SignUpViewModel", "💾 SharedPreferences에 저장 완료")

                // 성공
                _signUpState.value = SignUpState.Success("회원가입이 완료되었습니다!")

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("422") == true -> "입력 데이터가 올바르지 않습니다. 비밀번호는 영문과 숫자를 포함해야 합니다."
                    e.message?.contains("409") == true -> "이미 존재하는 이메일 또는 사원ID입니다."
                    e.message?.contains("connection") == true -> "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."
                    else -> e.message ?: "회원가입 실패. 다시 시도해주세요."
                }
                _signUpState.value = SignUpState.Error(errorMessage)
            }
        }
    }
}