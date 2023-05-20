package com.aomined.synclibrary.ui.viewholder

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.aomined.synclibrary.R
import com.aomined.synclibrary.SyncUsers
import com.aomined.synclibrary.data.UserSession
import com.aomined.synclibrary.data.isOnline
import com.aomined.synclibrary.databinding.ItEmMembErsBinding

class UserSessionViewHolder(private val binding: ItEmMembErsBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(userSession: UserSession, isHost: Boolean, hostId: String, callback:() -> Unit) {
        // Aquí puedes configurar los valores de las vistas con los datos del usuario
        binding.tvName.text = userSession.name

        // Configuramos la visibilidad del icono de expulsión
        binding.expulseIcon.isVisible = (isHost && userSession.id != hostId)

        if(binding.expulseIcon.isVisible){
            binding.expulseIcon.setOnClickListener {
                callback()
            }
        }

        val context = binding.hostIcon.context

        if(userSession.readyForPlay) {
            val drawableStatus = ContextCompat.getDrawable(
                context,
                if (userSession.isOnline()) R.drawable.w_online_status else R.drawable.w_offline_status
            )
            binding.userStatus.setImageDrawable(drawableStatus)
        }else{
            val drawableStatus = ContextCompat.getDrawable(
                context,
                 R.drawable.w_not_ready_status
            )
            binding.userStatus.setImageDrawable(drawableStatus)
        }

        val myUserId = SyncUsers.getInstance().getMyUserId()
        binding.tvName.setTextColor(ContextCompat.getColor(context, if(userSession.id == myUserId) R.color.accent else R.color.white ))
        val drawableToUse =  ContextCompat.getDrawable(context, if(userSession.id == hostId) R.drawable.hos_t_ico_n else R.drawable.u_s_er_icon)
        binding.hostIcon.setImageDrawable(drawableToUse)
        binding.hostIcon.isVisible =  true
    }
}