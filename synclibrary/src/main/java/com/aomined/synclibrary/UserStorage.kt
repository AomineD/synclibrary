package com.aomined.synclibrary

import android.content.Context
import android.content.SharedPreferences
import com.aomined.synclibrary.data.UserSession
import com.google.gson.Gson

class UserStorage private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_storage", Context.MODE_PRIVATE)

    companion object {
        private var instance: UserStorage? = null

        @Synchronized
        fun getInstance(context: Context? = null): UserStorage {
            if (instance == null && context != null) {
                instance = UserStorage(context.applicationContext)
            }else if(instance == null){
                instance = UserStorage(Watch2Application.instance)
            }
            return instance as UserStorage
        }
    }

    fun getUserId(): String {
        var userId = sharedPreferences.getString("user_id", null)
        if (userId == null) {
            userId = generateUserId()
            sharedPreferences.edit().putString("user_id", userId).apply()
        }
        return userId
    }

    fun saveUserSession(userSession: UserSession) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        val gson = Gson()
        val userSessionJson = gson.toJson(userSession)
        editor.putString("user_session23", userSessionJson)
        editor.apply()
    }

    fun getUserSession(): UserSession? {
        val gson = Gson()
        val userSessionJson = sharedPreferences.getString("user_session23", null)
        return if (userSessionJson != null) {
            gson.fromJson(userSessionJson, UserSession::class.java)
        } else {
            null
        }
    }

    private fun generateUserId(): String {
        // Aquí puedes implementar la lógica para generar un ID único para el usuario
        return "user_${System.currentTimeMillis()}"
    }

    fun clearData(){
        sharedPreferences.edit().clear().apply()
    }
}
