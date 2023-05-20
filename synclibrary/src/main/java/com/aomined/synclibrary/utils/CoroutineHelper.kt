package com.aomined.synclibrary.utils

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object CoroutineHelper {
    fun <T> T.ioSafe(work: suspend (CoroutineScope.(T) -> Unit)):Job{
        val value = this

        return CoroutineScope(Dispatchers.IO).launch {
            try {
                work(value)
            }catch (t:Throwable){
                logerror(t)
            }
        }
    }

    fun <T> T.main(work: suspend (CoroutineScope.(T) -> Unit)):Job{
        val value = this

        return CoroutineScope(Dispatchers.Main).launch {
            try {
                work(value)
            }catch (t:Throwable){
                logerror(t)
            }
        }
    }

    /**
     * Log error orderly
     */
    private fun logerror(throwable: Throwable) {
        Log.d("Safe", "-------------------------------------------------------------------")
        Log.d("Safe", "safeLaunch: " + throwable.localizedMessage)
        Log.d("Safe", "safeLaunch: " + throwable.message)
        throwable.printStackTrace()
        Log.d("Safe", "-------------------------------------------------------------------")
    }


}