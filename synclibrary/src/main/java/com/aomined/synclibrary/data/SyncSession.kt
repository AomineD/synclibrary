package com.aomined.synclibrary.data

import android.util.Log
import com.aomined.synclibrary.SyncUsers

data class SyncSession(
    val sessionId: String = "",
    val hostId: String = "",
    val videoId: String = "",
    val videoOption:String = "",
    val videoTimestamp: Long = 0,
    val isPaused: Boolean = true,
    val lastOnlineHost:Long = 0,
    val timestamp: Long = 0,
    val memberList:MutableList<UserSession>? = null
)

fun SyncSession.iamInThisRoom(): Boolean {
    val myUser = SyncUsers.getInstance().getMyUserId()
    return memberList?.any { it.id == myUser } ?: false
}

fun SyncSession.hostIsOnline():Boolean{
    memberList?.forEach { user ->
        Log.e("MAIN", "hostIsOnline: host? ${user.id == hostId} userOnline? ${user.isOnline()}" )
        if(user.id == hostId && user.isOnline()){
            return true
        }
    } ?: return false

    return false
}