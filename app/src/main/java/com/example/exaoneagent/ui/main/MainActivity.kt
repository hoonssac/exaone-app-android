package com.example.exaoneagent.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.activity.viewModels
import com.example.exaoneagent.databinding.ActivityMainBinding
import com.example.exaoneagent.data.local.PreferenceManager
import com.example.exaoneagent.data.db.AppDatabase
import com.example.exaoneagent.ui.chat.ChatFragment
import com.example.exaoneagent.ui.chat.ChatViewModel
import com.example.exaoneagent.ui.chat.NavigationDrawerFragment
import com.example.exaoneagent.ui.login.LoginActivity
import com.example.exaoneagent.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private var navigationDrawerFragment: NavigationDrawerFragment? = null
    private val chatViewModel: ChatViewModel by viewModels()
    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupToolbar()
        loadFragments()
        setupDrawer()

        // 앱 시작 시 스레드 목록 동기화
        chatViewModel.syncThreadsFromServer()
        Log.d(tag, "🔄 스레드 동기화 시작")

        // 로그인 후 새 채팅 시작
        if (intent.getBooleanExtra("START_NEW_CHAT", false)) {
            chatViewModel.startNewChat()
            Log.d(tag, "✅ 새 채팅 시작")
        }

        // 현재 스레드 상태 감지하여 메뉴 갱신
        chatViewModel.currentThread.observe(this) { thread ->
            invalidateOptionsMenu() // onPrepareOptionsMenu 호출 유도
        }

        Log.d(tag, "✅ MainActivity 초기화 완료")
    }

    /**
     * 툴바 설정
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    /**
     * 드로어 설정
     */
    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.app_name,
            R.string.app_name
        )

        binding.drawerLayout.addDrawerListener(drawerToggle)

        // NavigationDrawerFragment의 드로어 닫기 콜백 연결
        if (navigationDrawerFragment != null) {
            navigationDrawerFragment?.onDrawerClosing = {
                binding.drawerLayout.closeDrawer(binding.navigationDrawer)
                Log.d(tag, "🔒 드로어 닫음")
            }
        }

        drawerToggle.syncState()

        Log.d(tag, "✅ 드로어 설정 완료")
    }

    /**
     * Fragment 로드
     */
    private fun loadFragments() {
        // ChatFragment 로드
        val chatFragment = ChatFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .commit()

        // NavigationDrawerFragment 로드
        navigationDrawerFragment = NavigationDrawerFragment()
        navigationDrawerFragment?.apply {
            // 스레드 선택 시 드로어 닫기
            onThreadSelected = { threadId ->
                Log.d(tag, "스레드 선택됨: $threadId")
                // NavigationDrawerFragment에서 이미 selectThread/startNewThread가 호출됨
                // 여기서는 드로어만 닫기
                binding.drawerLayout.closeDrawer(binding.navigationDrawer)
            }
        }

        // 드로어 Fragment 컨테이너에 NavigationDrawerFragment 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.drawer_fragment_container, navigationDrawerFragment!!)
            .commit()

        Log.d(tag, "✅ Fragment 로드 완료")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val deleteItem = menu?.findItem(R.id.menu_delete_thread)
        // 현재 선택된 스레드가 있으면(null이 아니면) 삭제 메뉴 표시
        val currentThread = chatViewModel.currentThread.value
        if (currentThread != null) {
            deleteItem?.isVisible = true
            
            // 텍스트를 빨간색으로 강조
            val spanString = android.text.SpannableString(deleteItem?.title.toString())
            spanString.setSpan(
                android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#E02020")), 
                0, 
                spanString.length, 
                0
            )
            deleteItem?.title = spanString
        } else {
            deleteItem?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                val intent = Intent(this, com.example.exaoneagent.ui.guide.GuideActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_logout -> {
                logout()
                true
            }
            R.id.menu_delete_thread -> {
                chatViewModel.currentThread.value?.let { thread ->
                    // 삭제 확인 다이얼로그 (Material Style)
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("스레드 삭제")
                        .setMessage("'${thread.title}'을(를) 삭제하시겠습니까?")
                        .setPositiveButton("삭제") { _, _ ->
                            chatViewModel.deleteThread(thread.id)
                            com.google.android.material.snackbar.Snackbar.make(binding.root, "스레드가 삭제되었습니다", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
                true
            }
            android.R.id.home -> {
                // 햄버거 메뉴 버튼 클릭 (drawerToggle이 처리)
                if (binding.drawerLayout.isDrawerOpen(binding.navigationDrawer)) {
                    binding.drawerLayout.closeDrawer(binding.navigationDrawer)
                } else {
                    binding.drawerLayout.openDrawer(binding.navigationDrawer)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * 로그아웃
     */
    private fun logout() {
        lifecycleScope.launch {
            try {
                // 로컬 Room DB 초기화 - IO Thread에서 실행
                withContext(Dispatchers.IO) {
                    val database = AppDatabase.getInstance(this@MainActivity)
                    database.threadDao().deleteAllThreads()
                    database.messageDao().deleteAllMessages()
                    Log.d(tag, "✅ 로컬 DB 초기화 완료")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ 로컬 DB 초기화 실패: ${e.message}")
            }

            // 토큰 및 사용자 정보 삭제
            preferenceManager.clearAll()

            // 로그인 화면으로 이동
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("SHOW_LOGOUT_MESSAGE", true)
            startActivity(intent)
            finish()

            // Toast 대신 Snackbar 사용 (아이콘 문제 해결 및 디자인 개선)
            // 뷰가 이미 destroy 되었을 수 있으므로, LoginActivity에서 띄우거나 여기서 Application Context로 띄우는 건 제한적임.
            // 하지만 로그아웃 후 액티비티가 종료되므로, 스낵바를 띄울 뷰가 사라집니다.
            // 따라서 "로그아웃 되었습니다" 메시지는 LoginActivity로 이동한 후에 띄우는 것이 가장 자연스럽습니다.
            // 여기서는 Intent에 플래그를 담아 보냅니다.
            Log.d(tag, "로그아웃 완료")
        }
    }

    override fun onBackPressed() {
        // 드로어가 열려있으면 닫고, 아니면 뒤로가기
        if (binding.drawerLayout.isDrawerOpen(binding.navigationDrawer)) {
            binding.drawerLayout.closeDrawer(binding.navigationDrawer)
        } else {
            super.onBackPressed()
        }
    }
}
