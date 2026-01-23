package com.example.exaoneagent.ui.signup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.exaoneagent.databinding.ActivitySignUpBinding
import com.example.exaoneagent.ui.login.LoginActivity
import com.example.exaoneagent.ui.main.MainActivity

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var viewModel: SignUpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel 초기화 (Context 전달)
        viewModel = SignUpViewModel(this)

        setupDropdownMenus()
        setupListeners()
        observeViewModel()
    }

    private fun setupDropdownMenus() {
        val departments = arrayOf("AI 연구소", "경영지원팀", "플랫폼개발팀", "영업본부", "기획팀")
        val deptAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
        binding.spinnerDept.setAdapter(deptAdapter)

        val positions = arrayOf("사원", "대리", "과장", "차장", "부장", "이사")
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, positions)
        binding.spinnerPosition.setAdapter(posAdapter)
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        binding.btnSignUp.setOnClickListener {
            handleSignUp()
        }

        // 입력 필드 감시
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSignUpButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etName.addTextChangedListener(textWatcher)
        binding.etEmployeeId.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)

        // 스피너 변화 감시
        binding.spinnerDept.setOnItemClickListener { _, _, _, _ ->
            updateSignUpButtonState()
        }
        binding.spinnerPosition.setOnItemClickListener { _, _, _, _ ->
            updateSignUpButtonState()
        }

        // 초기 상태 설정
        updateSignUpButtonState()
    }

    private fun updateSignUpButtonState() {
        val email = binding.etEmail.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val employeeId = binding.etEmployeeId.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val dept = binding.spinnerDept.text.toString().trim()
        val pos = binding.spinnerPosition.text.toString().trim()

        val isAllFieldsFilled = email.isNotEmpty() &&
                                name.isNotEmpty() &&
                                employeeId.isNotEmpty() &&
                                password.isNotEmpty() &&
                                dept.isNotEmpty() &&
                                pos.isNotEmpty()

        binding.btnSignUp.isEnabled = isAllFieldsFilled

        if (isAllFieldsFilled) {
            // 활성화: 그라데이션 배경
            binding.btnSignUp.setBackgroundResource(com.example.exaoneagent.R.drawable.gradient_primary)
            binding.btnSignUp.setTextColor(getColor(android.R.color.white))
        } else {
            // 비활성화: 회색 배경 (곡률 포함)
            binding.btnSignUp.setBackgroundResource(com.example.exaoneagent.R.drawable.button_disabled)
            binding.btnSignUp.setTextColor(getColor(android.R.color.white))
        }
    }

    private fun observeViewModel() {
        viewModel.signUpState.observe(this) { state ->
            when (state) {
                is SignUpState.Loading -> {
                    binding.btnSignUp.isEnabled = false
                    binding.btnSignUp.text = "회원가입 중..."
                }
                is SignUpState.Success -> {
                    com.google.android.material.snackbar.Snackbar.make(binding.root, state.message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                    // MainActivity로 이동
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                is SignUpState.Error -> {
                    binding.btnSignUp.text = "가입 완료"
                    com.google.android.material.snackbar.Snackbar.make(binding.root, "회원가입 실패: ${state.message}", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                    updateSignUpButtonState()
                }
                else -> {
                    updateSignUpButtonState()
                }
            }
        }
    }

    private fun handleSignUp() {
        // 입력 값 가져오기
        val email = binding.etEmail.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val employeeId = binding.etEmployeeId.text.toString().trim()  // 사원ID 추가
        val dept = binding.spinnerDept.text.toString()
        val pos = binding.spinnerPosition.text.toString()

        // 유효성 검사
        if (email.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "이메일을 입력해주세요.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "올바른 이메일 형식을 입력해주세요.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }

        if (name.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "이름을 입력해주세요.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }

        if (employeeId.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "사원ID를 입력해주세요.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "비밀번호를 입력해주세요.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }

        if (password.length < 8) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "비밀번호는 8자 이상이어야 합니다.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }

        // 비밀번호: 영문과 숫자 포함 검증
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        if (!hasLetter || !hasDigit) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "비밀번호는 영문과 숫자를 포함해야 합니다.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }

        if (dept.isEmpty() || pos.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, "부서와 직급을 선택해주세요.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }

        // ViewModel의 signUp 호출
        viewModel.signUp(email, name, password, employeeId, dept, pos)
    }
}
