package com.aomined.synclibrary.listeners

interface UserListener {
    fun onSuccessLogin()
    fun onError(e:Exception)
}