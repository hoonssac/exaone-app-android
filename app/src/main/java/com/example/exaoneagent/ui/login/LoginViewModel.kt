package com.example.exaoneagent.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.exaoneagent.network.RetrofitClient
import com.example.exaoneagent.network.dto.LoginRequest
import com.example.exaoneagent.data.local.PreferenceManager
import android.content.Context
import android.util.Log

class LoginViewModel(val context: Context) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    private val preferenceManager = PreferenceManager(context)

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                // API 호출
                val request = LoginRequest(
                    email = email,
                    password = password
                )

                val response = RetrofitClient.apiService.login(request)

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

                // 로그 출력
                Log.d("LoginViewModel", "✅ 로그인 성공!")
                Log.d("LoginViewModel", "🔐 Access Token: ${response.accessToken.substring(0, 20)}...")
                Log.d("LoginViewModel", "👤 사용자: ${response.user.email} (${response.user.name})")
                Log.d("LoginViewModel", "💾 SharedPreferences에 저장 완료")

                // 성공
                _loginState.value = LoginState.Success("로그인 성공")

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("401") == true -> "이메일 또는 비밀번호가 잘못되었습니다."
                    e.message?.contains("connection") == true -> "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."
                    else -> e.message ?: "로그인 중 오류가 발생했습니다"
                }
                Log.e("LoginViewModel", "❌ 로그인 실패: $errorMessage")
                _loginState.value = LoginState.Error(errorMessage)
            }
        }
    }
}
