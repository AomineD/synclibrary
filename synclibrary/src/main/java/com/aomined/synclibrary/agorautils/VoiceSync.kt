package com.aomined.synclibrary.agorautils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.aomined.synclibrary.R
import com.aomined.synclibrary.TAG
import com.aomined.synclibrary.UserStorage
import com.aomined.synclibrary.data.StatusSync
import com.aomined.synclibrary.data.SyncResponse
import com.aomined.synclibrary.ui.viewmodel.SyncViewModel
import com.aomined.synclibrary.utils.UtilsHelper.appId
import io.agora.rtc2.*


class VoiceSync(private val context: Context) {

    companion object{
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance:VoiceSync

        fun init(context: Context){
            instance = VoiceSync(context)
            instance.setupVoiceSDKEngine()
        }

        fun getInstance():VoiceSync{
            return instance
        }
    }

    private var syncViewModel:SyncViewModel? = null

    fun setViewModel(syncViewModel: SyncViewModel){
        this.syncViewModel = syncViewModel
    }

    // An integer that identifies the local user.

    // Track the status of your connection
    private var isJoined = false

    // Agora engine instance
    private var agoraEngine: RtcEngine? = null

    private var callbackResponse:(SyncResponse) -> Unit = {

    }

    private fun setupVoiceSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            Log.e(TAG, "setupVoiceSDKEngine: configured!" )
        } catch (e: Exception) {
            Log.e(TAG, "setupVoiceSDKEngine: ${e.message}" )
        }
    }

    fun joinChannel(channelName:String, callback: (SyncResponse) -> Unit) {

        val user = UserStorage.getInstance(context)

        user.getUserSession()?.let {userMy ->
            if(agoraEngine == null){
                callback(SyncResponse(StatusSync.ERROR, context.getString(R.string.w_agora_engine_error)))
                return@let
            }

            if(syncViewModel == null){
                callback(SyncResponse(StatusSync.ERROR, context.getString(R.string.w_vidwmodel_error)))
                return@let
            }

            callbackResponse = callback

            callback(SyncResponse(StatusSync.LOADING))
            syncViewModel!!.loadToken(channelName){
                joinWith(it, channelName, userMy.uniqueId)
            }



        } ?: run {
            callback(SyncResponse(StatusSync.ERROR, context.getString(R.string.w_user_null_error)))
        }
        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.


    }

    private fun joinWith(token:String, channelName: String, uniqueId:Int){

        val options = ChannelMediaOptions()
        options.autoSubscribeAudio = true
        // Set both clients as the BROADCASTER.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Set the channel profile as BROADCASTING.
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        agoraEngine!!.joinChannel(token, channelName, uniqueId, options)
        Log.e(TAG, "joinWith: entrando... con token $token" )
    }

    fun leaveChannel(){
        if(isJoined) {
            agoraEngine?.let {
                it.leaveChannel()
            }
            callbackResponse(SyncResponse(StatusSync.DISCONNECTED))
            callbackResponse = {

            }
        }
    }

    fun muteOrUnmute(mute:Boolean){
        agoraEngine?.let {
            it.muteLocalAudioStream(mute)
        }
    }

    fun muteUnmuteRemoteAudio(mute: Boolean) {
        agoraEngine?.let {
            val volume = if (mute) 0 else 100
            it.adjustPlaybackSignalVolume(volume)
        }
    }


    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote user joining the channel.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.e("MAIN", "onUserJoined: $uid" )
            callbackResponse(SyncResponse(StatusSync.NEW_USER))
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            // Successfully joined a channel
            isJoined = true
            callbackResponse(SyncResponse(StatusSync.OK))
            Log.e("MAIN", "onJoinChannelSuccess: yes $channel - $uid" )
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            // Listen for remote users leaving the channel
            Log.e("MAIN", "onUserOffline: $reason" )
        }

        override fun onLeaveChannel(stats: RtcStats) {
            // Listen for the local user leaving the channel
            Log.e("MAIN", "onLeaveChannel: $stats" )
            isJoined = false
        }

        override fun onError(err: Int) {
            super.onError(err)
            Log.e(TAG, "onError: $err" )
            callbackResponse(SyncResponse(StatusSync.ERROR, "Error: $err"))
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Log.e(TAG, "onConnectionLost: connectionLost" )
            callbackResponse(SyncResponse(StatusSync.DISCONNECTED))
        }
    }

    fun destroy(){
        callbackResponse = {

        }
        agoraEngine?.leaveChannel()
        RtcEngine.destroy()
        agoraEngine = null
    }

}