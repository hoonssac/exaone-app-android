package com.example.exaoneagent.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.exaoneagent.databinding.ActivityLoginBinding
import com.example.exaoneagent.ui.main.MainActivity
import com.example.exaoneagent.ui.signup.SignUpActivity
import com.example.exaoneagent.data.local.PreferenceManager
import com.example.exaoneagent.data.db.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Context를 전달하여 ViewModel 초기화
        viewModel = LoginViewModel(this)
        preferenceManager = PreferenceManager(this)

        // 자동 로그인 확인: 토큰이 저장되어 있으면 MainActivity로 이동
        checkAutoLogin()

        // 로그아웃 메시지 확인
        if (intent.getBooleanExtra("SHOW_LOGOUT_MESSAGE", false)) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "로그아웃되었습니다", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }

        setupObservers()
        setupListeners()
    }

    private fun checkAutoLogin() {
        val accessToken = preferenceManager.getAccessToken()
        if (accessToken != null && accessToken.isNotEmpty()) {
            // 토큰이 존재하면 자동으로 MainActivity로 이동
            navigateToMain()
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.alpha = 0.6f
                    binding.progressBar.visibility = android.view.View.VISIBLE
                }
                is LoginState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    com.google.android.material.snackbar.Snackbar.make(binding.root, state.message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is LoginState.Error -> {
                    updateLoginButtonState()
                    binding.btnLogin.alpha = 1f
                    binding.progressBar.visibility = android.view.View.GONE
                    com.google.android.material.snackbar.Snackbar.make(binding.root, state.message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                }
                is LoginState.Idle -> {
                    updateLoginButtonState()
                    binding.btnLogin.alpha = 1f
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun updateLoginButtonState() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        val isEnabled = email.isNotEmpty() && password.isNotEmpty()
        binding.btnLogin.isEnabled = isEnabled

        if (isEnabled) {
            binding.btnLogin.setBackgroundResource(com.example.exaoneagent.R.drawable.gradient_primary)
        } else {
            binding.btnLogin.setBackgroundResource(com.example.exaoneagent.R.drawable.button_disabled)
        }
    }

    private fun setupListeners() {
        // 입력 필드 감시
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLoginButtonState()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)

        // 로그인 버튼 클릭 시
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInput(email, password)) {
                viewModel.login(email, password)
            }
        }

        // 회원가입 텍스트 클릭 시 (기존 "준비중" 토스트에서 화면 전환으로 수정)
        // 레이아웃의 ID가 tvForgotPassword로 되어 있으므로 이를 그대로 활용합니다.
        binding.tvForgotPassword.setOnClickListener {
            // 이제 SignUpActivity가 임포트되어 에러가 발생하지 않습니다.
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "이메일을 입력해주세요", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            binding.etEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "올바른 이메일 형식을 입력해주세요", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            binding.etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "비밀번호를 입력해주세요", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun navigateToMain() {
        lifecycleScope.launch {
            try {
                // 로컬 Room DB 초기화 (로그인 시 캐시 제거) - IO Thread에서 실행
                withContext(Dispatchers.IO) {
                    val database = AppDatabase.getInstance(this@LoginActivity)
                    database.threadDao().deleteAllThreads()
                    database.messageDao().deleteAllMessages()
                    Log.d("LoginActivity", "✅ 로컬 DB 초기화 완료")
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ 로컬 DB 초기화 실패: ${e.message}")
            }

            // MainActivity로 이동
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            intent.putExtra("START_NEW_CHAT", true)  // 새 채팅 시작 플래그
            startActivity(intent)
            finish()
        }
    }
}