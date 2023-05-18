package com.aomined.synclibrary.data

import android.util.Log
import com.aomined.synclibrary.TAG
import com.aomined.synclibrary.onlineThreshold

data class UserSession(
    val id:String,
    val name:String,
    val signUpMillis:Long? = null,
    val lastOnline: Long = 0,
    val uniqueId:Int = -1,
    var readyForPlay:Boolean = false,
){
    constructor() : this("", "", null) {
        // Inicializa manualmente los campos de la clase que no se inicializaron en el constructor primario
    }
}

fun UserSession.setReady(){
    this.readyForPlay = true
}

fun UserSession.isOnline():Boolean{
    val currentTime = System.currentTimeMillis()
    //Log.e(TAG, "isOnline: ${currentTime - lastOnline}" )
    return currentTime - lastOnline <= onlineThreshold
}
