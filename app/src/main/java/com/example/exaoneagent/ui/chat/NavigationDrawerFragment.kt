package com.example.exaoneagent.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AlertDialog
import com.example.exaoneagent.databinding.FragmentNavigationDrawerBinding
import com.example.exaoneagent.ui.chat.adapter.ThreadAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NavigationDrawerFragment : Fragment() {

    private var _binding: FragmentNavigationDrawerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var threadAdapter: ThreadAdapter
    private val tag = "NavigationDrawer"

    // 스레드 선택 콜백
    var onThreadSelected: ((Long) -> Unit)? = null

    // 드로어 닫기 콜백
    var onDrawerClosing: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        Log.d(tag, "✅ NavigationDrawerFragment 초기화 완료")
    }


    /**
     * RecyclerView 설정
     */
    private fun setupRecyclerView() {
        threadAdapter = ThreadAdapter(
            onThreadClick = { thread ->
                viewModel.selectThread(thread)
                onThreadSelected?.invoke(thread.id)
                Log.d(tag, "✅ 스레드 선택: ${thread.id}")
            },
            onThreadLongClick = { thread ->
                // 스레드 삭제 확인 Dialog (Material Style)
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("스레드 삭제")
                    .setMessage("'${thread.title}'을(를) 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        viewModel.deleteThread(thread.id)
                        com.google.android.material.snackbar.Snackbar.make(binding.root, "스레드가 삭제되었습니다", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                        Log.d(tag, "🗑️ 스레드 삭제: ${thread.id}")
                    }
                    .setNegativeButton("취소", null)
                    .show()
                true  // long click 처리됨
            }
        )

        binding.rvThreads.apply {
            adapter = threadAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /**
     * ViewModel 옵저버 설정
     */
    private fun setupObservers() {
        viewModel.allThreads.observe(viewLifecycleOwner) { threads ->
            threadAdapter.submitList(threads)

            // 스레드 개수에 따라 메시지 표시
            if (threads.isEmpty()) {
                binding.tvEmptyMessage.visibility = View.VISIBLE
                binding.rvThreads.visibility = View.GONE
            } else {
                binding.tvEmptyMessage.visibility = View.GONE
                binding.rvThreads.visibility = View.VISIBLE
            }

            Log.d(tag, "✅ ${threads.size}개의 스레드 표시 중")
        }

        // 현재 선택된 스레드 감시하여 UI 하이라이트
        viewModel.currentThread.observe(viewLifecycleOwner) { thread ->
            threadAdapter.setSelectedThread(thread?.id)
        }
    }

    /**
     * 버튼 리스너 설정
     */
    private fun setupListeners() {
        // 새 채팅 버튼 (텍스트)
        binding.btnNewChat.setOnClickListener {
            viewModel.startNewChat()
            onDrawerClosing?.invoke()  // 드로어 닫기
            Log.d(tag, "새 채팅 시작 버튼 클릭")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
