package com.aomined.synclibrary.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aomined.synclibrary.data.UserSession
import com.aomined.synclibrary.databinding.ItEmMembErsBinding
import com.aomined.synclibrary.ui.viewholder.UserSessionViewHolder

class UserSessionAdapter(
    private val userSessions: List<UserSession>,
    private val isHost: Boolean,
    private val hostId: String,
    private val expulseCallback:(Int) -> Unit
) : RecyclerView.Adapter<UserSessionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSessionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItEmMembErsBinding.inflate(layoutInflater, parent, false)
        return UserSessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserSessionViewHolder, position: Int) {
        val userSession = userSessions[position]
        holder.bind(userSession, isHost, hostId){
            expulseCallback(position)
        }
    }

    override fun getItemCount() = userSessions.size
}