package com.aomined.synclibrary.listeners

import com.aomined.synclibrary.data.SyncResponse
import com.aomined.synclibrary.data.SyncSession

interface SyncSessionListener {
    fun onSuccess(session:SyncSession?)
    fun onFailure(e:Exception)
    fun onSessionTerminated()
    fun onVoiceStateChanged(syncResponse: SyncResponse)
    fun onNotice(type:String)
    fun onChatStateChanged(isSuccess:Boolean)
}