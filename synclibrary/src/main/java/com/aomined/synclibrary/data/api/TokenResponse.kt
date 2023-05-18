package com.aomined.synclibrary.data.api

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("token") val token:String?
)
