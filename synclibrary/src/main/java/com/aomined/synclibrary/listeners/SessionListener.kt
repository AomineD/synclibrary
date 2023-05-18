package com.aomined.synclibrary.listeners

import com.aomined.synclibrary.data.SyncSession

interface SessionListener {
    fun onUser(userEnter: Boolean, userId:String)
    fun onSessionUpdated(updated: SyncSession)
    fun onSessionError(error:String?)
    fun onSessionDestroyed()
}