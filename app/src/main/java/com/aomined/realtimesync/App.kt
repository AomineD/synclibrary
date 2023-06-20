package com.aomined.realtimesync

import android.app.Activity
import android.os.Bundle
import com.aomined.synclibrary.Watch2Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App:Watch2Application() {
    override fun onSetAppId(): String {
        return "9750698e742a4cdb8cc1ce5ff593ab97"
    }

    override fun onSetAppKeyChat(): String {
        return "41932986#1090222"
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityCreated(activity, savedInstanceState)
    }

    override fun onActivityStarted(activity: Activity) {
        super.onActivityStarted(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        super.onActivityStopped(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        super.onActivityDestroyed(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        super.onActivitySaveInstanceState(activity, outState)
    }
}