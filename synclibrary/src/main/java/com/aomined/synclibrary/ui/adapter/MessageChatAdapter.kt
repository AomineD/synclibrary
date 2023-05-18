package com.aomined.synclibrary.ui.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aomined.synclibrary.R
import com.aomined.synclibrary.Watch2Application
import com.aomined.synclibrary.chat.ChatMessage
import com.aomined.synclibrary.chat.formattedDateTime
import com.aomined.synclibrary.chat.isMine
import com.aomined.synclibrary.databinding.MSgITEmBinding

class MessageChatAdapter(private val chatMessages: List<ChatMessage>) :
    RecyclerView.Adapter<MessageChatAdapter.MessageChatViewHolder>() {

    inner class MessageChatViewHolder(val binding: MSgITEmBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageChatViewHolder {
        val binding = MSgITEmBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageChatViewHolder, position: Int) {
        val chatMessage = chatMessages[position]

        val context = holder.binding.hourTvOutcoming.context
        if (chatMessage.isMine()) {
            holder.binding.messageIncomingLayout.visibility = View.GONE
            holder.binding.messageOutcomingLayout.visibility = View.VISIBLE

            holder.binding.messageTvOutcoming.text = chatMessage.message
            holder.binding.hourTvOutcoming.text = chatMessage.formattedDateTime(context)
        } else {
            holder.binding.messageIncomingLayout.visibility = View.VISIBLE
            holder.binding.messageOutcomingLayout.visibility = View.GONE

            // Aquí puedes cargar la imagen del usuario en avatarUser si es necesario
            holder.binding.nameTvIncoming.text = chatMessage.senderName
            holder.binding.messageTvIncoming.text = chatMessage.message
            holder.binding.hourTvIncoming.text = chatMessage.formattedDateTime(context)
        }
    }

    override fun getItemCount() = chatMessages.size
}