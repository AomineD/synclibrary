package com.aomined.synclibrary.api

import com.aomine.playdedeapi.api.ApiInterface
import javax.inject.Inject

class ApiService @Inject constructor(private val apiInterface: ApiInterface) {

    suspend fun generateToken(channelName:String, userId:String) = apiInterface.generateToken(channelName, userId)

    suspend fun generateForChat(userId:String) = apiInterface.generateTokenForChat(userId)
}