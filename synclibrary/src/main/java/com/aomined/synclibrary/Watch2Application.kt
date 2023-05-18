package com.aomined.synclibrary

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.aomined.synclibrary.agorautils.VoiceSync
import com.aomined.synclibrary.utils.UtilsHelper.appId
import com.aomined.synclibrary.utils.UtilsHelper.appKeyChat

open class Watch2Application : Application(), Application.ActivityLifecycleCallbacks {

    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var instance:Watch2Application
    }

    fun setActivityWatch2Getter(){
        watchActivity = currentActivity
    }

    private var watchActivity:Activity? = null
    private var currentActivity:Activity? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        appId = onSetAppId()
        appKeyChat = onSetAppKeyChat()
        VoiceSync.init(this)

        registerActivityLifecycleCallbacks(this)
    }

    open fun onSetAppId():String
    {
        return appId
    }

    open fun onSetAppKeyChat():String{
        return appKeyChat
    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Se llama cuando se crea una Activity
    }

    override fun onActivityStarted(activity: Activity) {
        // Se llama cuando se inicia una Activity
    }

    override fun onActivityResumed(activity: Activity) {
        // Se llama cuando se reanuda una Activity
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        // Se llama cuando se pausa una Activity
    }

    override fun onActivityStopped(activity: Activity) {
        // Se llama cuando se detiene una Activity

    }

    override fun onActivityDestroyed(activity: Activity) {
        // Se llama cuando se destruye una Activity
        if(activity.javaClass == watchActivity?.javaClass){
            // watch se detuvó
            watchActivity = null
            SyncUsers.getInstance().onDisconnectDevice(SyncUsers.getInstance().isHost())
            VoiceSync.getInstance().destroy()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Se llama cuando se guarda el estado de una Activity
    }
}
