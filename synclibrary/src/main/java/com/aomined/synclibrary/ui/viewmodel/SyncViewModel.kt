package com.aomined.synclibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.aomined.synclibrary.UserStorage
import com.aomined.synclibrary.Watch2Application
import com.aomined.synclibrary.api.ApiService
import com.aomined.synclibrary.utils.CoroutineHelper.ioSafe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(private val apiService: ApiService):ViewModel() {

    fun loadToken(channel:String, response:(String) -> Unit){
        ioSafe {
            val user = UserStorage.getInstance(Watch2Application.instance).getUserSession()

            user?.let {
                val tokenResponse = apiService.generateToken(channel, user.uniqueId.toString())
                response(tokenResponse.token ?: "no-token")
            } ?: run {
                response("no-token")
            }
        }
    }

    fun loadChatToken(response:(String) -> Unit){
        ioSafe {
            val user = UserStorage.getInstance(Watch2Application.instance).getUserSession()

            user?.let {
                val tokenResponse = apiService.generateForChat("${user.uniqueId}##${user.name}")
                response(tokenResponse.token ?: "no-token")
            } ?: run {
                response("no-token")
            }
        }
    }
}