package com.aomine.playdedeapi.api

import com.aomined.synclibrary.data.api.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 pddkit/2023"


interface ApiInterface {

    @Headers("User-Agent: $USER_AGENT")
    @FormUrlEncoded
    @POST("generate_token")
    suspend fun generateToken(@Field("chann_name") channelName:String, @Field("usr_id") userId:String): TokenResponse

    @Headers("User-Agent: $USER_AGENT")
    @FormUrlEncoded
    @POST("generate_token_chat")
    suspend fun generateTokenForChat(@Field("usr_id") userId:String): TokenResponse
}