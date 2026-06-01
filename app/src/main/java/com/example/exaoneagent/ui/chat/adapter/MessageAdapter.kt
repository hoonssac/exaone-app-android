package com.example.exaoneagent.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exaoneagent.data.db.entity.MessageEntity
import com.example.exaoneagent.databinding.ItemUserMessageBinding
import com.example.exaoneagent.databinding.ItemAssistantMessageBinding

class MessageAdapter : ListAdapter<MessageEntity, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).role) {
            "user" -> VIEW_TYPE_USER
            "assistant" -> VIEW_TYPE_ASSISTANT
            "loading" -> VIEW_TYPE_LOADING
            else -> VIEW_TYPE_USER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> UserMessageViewHolder(ItemUserMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            VIEW_TYPE_ASSISTANT -> AssistantMessageViewHolder(ItemAssistantMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            VIEW_TYPE_LOADING -> LoadingViewHolder(LayoutInflater.from(parent.context).inflate(com.example.exaoneagent.R.layout.item_loading_message, parent, false))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AssistantMessageViewHolder -> holder.bind(message)
            is LoadingViewHolder -> {}
        }
    }

    class UserMessageViewHolder(private val binding: ItemUserMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: MessageEntity) {
            binding.tvMessage.text = message.message
        }
    }

    class AssistantMessageViewHolder(private val binding: ItemAssistantMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: MessageEntity) {
            binding.tvMessage.text = message.message
            val correctedMsgParent = binding.tvCorrectedMsg.parent as? android.view.ViewGroup
            correctedMsgParent?.visibility = android.view.View.GONE
            val sqlParent = binding.tvSql.parent as? android.view.ViewGroup
            sqlParent?.visibility = android.view.View.GONE
        }

        fun updateText(text: String) {
            binding.tvMessage.text = text
        }
    }

    class LoadingViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView)

    class MessageDiffCallback : DiffUtil.ItemCallback<MessageEntity>() {
        override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity) = oldItem.id == newItem.id
        // resultData 등 UI에 표시되지 않는 필드 변경 시 불필요한 리바인드 방지
        override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
            oldItem.role == newItem.role && oldItem.message == newItem.message
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
        private const val VIEW_TYPE_LOADING = 3
    }

    fun resetAnimationState() {}
}
