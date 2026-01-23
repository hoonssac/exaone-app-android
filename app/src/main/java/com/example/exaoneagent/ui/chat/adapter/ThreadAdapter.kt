package com.example.exaoneagent.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exaoneagent.data.db.entity.ThreadEntity
import com.example.exaoneagent.databinding.ItemThreadBinding
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Locale

class ThreadAdapter(
    private val onThreadClick: (ThreadEntity) -> Unit,
    private val onThreadLongClick: (ThreadEntity) -> Boolean = { false }
) : ListAdapter<ThreadEntity, ThreadAdapter.ThreadViewHolder>(ThreadDiffCallback()) {

    private var selectedThreadId: Long? = null

    fun setSelectedThread(threadId: Long?) {
        val oldSelectedId = selectedThreadId
        selectedThreadId = threadId
        
        // 전체를 갱신하거나, 이전/현재 선택된 아이템만 효율적으로 갱신할 수 있습니다.
        // 여기서는 단순함을 위해 전체 갱신을 사용합니다. (목록이 작을 것으로 예상)
        notifyDataSetChanged()
    }

    inner class ThreadViewHolder(
        private val binding: ItemThreadBinding,
        private val onThreadClick: (ThreadEntity) -> Unit,
        private val onThreadLongClick: (ThreadEntity) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(thread: ThreadEntity) {
            val isSelected = thread.id == selectedThreadId
            
            binding.tvTitle.text = thread.title
            
            // 선택 상태에 따른 스타일 변경
            if (isSelected) {
                binding.root.setBackgroundResource(com.example.exaoneagent.R.drawable.bg_thread_selected)
                binding.tvTitle.setTextColor(binding.root.context.getColor(com.example.exaoneagent.R.color.exaone_primary))
                binding.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                binding.root.setBackgroundResource(android.R.color.transparent)
                binding.tvTitle.setTextColor(binding.root.context.getColor(com.example.exaoneagent.R.color.text_primary))
                binding.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            binding.root.setOnClickListener {
                onThreadClick(thread)
            }

            binding.root.setOnLongClickListener {
                onThreadLongClick(thread)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val binding = ItemThreadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThreadViewHolder(binding, onThreadClick, onThreadLongClick)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ThreadDiffCallback : DiffUtil.ItemCallback<ThreadEntity>() {
        override fun areItemsTheSame(oldItem: ThreadEntity, newItem: ThreadEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ThreadEntity, newItem: ThreadEntity): Boolean {
            return oldItem == newItem
        }
    }
}
