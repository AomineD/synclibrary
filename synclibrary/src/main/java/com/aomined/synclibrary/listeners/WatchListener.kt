package com.aomined.synclibrary.listeners

import com.aomined.synclibrary.data.SyncSession

interface WatchListener {
    fun onUserLogged()
    fun onCreateSession(session: SyncSession)
    fun onJoinedSession(session: SyncSession)
}