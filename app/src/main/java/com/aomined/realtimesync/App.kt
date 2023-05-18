package com.aomined.realtimesync

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
}