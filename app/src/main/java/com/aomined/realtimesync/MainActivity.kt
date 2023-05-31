package com.aomined.realtimesync

import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.aomined.realtimesync.databinding.ActivityMainBinding
import com.aomined.synclibrary.UserStorage
import com.aomined.synclibrary.SyncUsers
import com.aomined.synclibrary.TAG
import com.aomined.synclibrary.data.SyncSession
import com.aomined.synclibrary.data.UserSession
import com.aomined.synclibrary.listeners.SessionListener
import com.aomined.synclibrary.listeners.SyncSessionListener
import com.aomined.synclibrary.listeners.UserListener
import com.aomined.synclibrary.listeners.WatchListener
import com.aomined.synclibrary.utils.CoroutineHelper.main
import com.aomined.synclibrary.utils.PopUpInit
import com.aomined.synclibrary.utils.UtilsHelper
import com.aomined.synclibrary.utils.UtilsHelper.REQUESTED_PERMISSIONS
import com.aomined.synclibrary.utils.UtilsHelper.isSync
import com.aomined.synclibrary.utils.UtilsHelper.requestPermissionsSync
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val handlerTiming = Handler(Looper.getMainLooper())

    private var currentSyncMillis = 0L

    private var isPlaying = false

    override fun onStop() {
        super.onStop()
        SyncUsers.getInstance().onDisconnectDevice()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        SyncUsers.setDebug()

        requestPermissionsSync()







        SyncUsers.getInstance().setSessionListener(object : SessionListener{
            override fun onUser(userEnter: Boolean, userId: String) {

            }

            override fun onSessionUpdated(updated: SyncSession) {
              //  Log.e(TAG, "onSessionUpdated: aja ${updated.videoTimestamp} - ${updated.isPaused}" )

                currentSyncMillis = updated.videoTimestamp

                if(!updated.isPaused && !SyncUsers.getInstance().isHost() && isReady && !isPlaying){
                //        Log.e(TAG, "onSessionUpdated: is not paused!" )
                        playPauseVideo()
                }else if(updated.isPaused && !SyncUsers.getInstance().isHost() && isReady && isPlaying){
                    playPauseVideo()
                }else if(!updated.isPaused && !SyncUsers.getInstance().isHost() && isReady && isPlaying){

                    if(!updated.isSync(currentMillis)){
                  //      Log.e(TAG, "onSessionUpdated: ${updated.videoTimestamp} - $currentMillis" )
                        main {
                            binding.videoView.seekTo(currentSyncMillis.toInt() + 1000)
                        }
                    }

                }
            }

            override fun onSessionError(error: String?) {

            }

            override fun onSessionDestroyed() {

            }
        })



        SyncUsers.getInstance().setWatchListener(object : WatchListener{
            override fun onUserLogged() {
                binding.watch2GetterBtn.setVideo("84847", "https://playfootball.uheist.com/mv/gameplay.mp4")
            }

            override fun onCreateSession(session: SyncSession) {
                initVideo(session.videoOption)
            }

            override fun onJoinedSession(session: SyncSession) {
                initVideo(session.videoOption)
            }
        })

        binding.videoView.setOnCompletionListener {
            isPlaying = false
        }

        binding.videoView.setOnClickListener {
            //Log.e(TAG, "onCreate: CLICK ${SyncUsers.getInstance().isHost()} && $isReady" )
            if(isReady && SyncUsers.getInstance().isHost()){
                playPauseVideo()
              //  Log.e(TAG, "isPlaying: $isPlaying" )
                SyncUsers.getInstance().syncVideoState(currentMillis, !isPlaying)
            }
        }

    }

    private var isReady = false
    private fun initVideo(withVideo:String){
        val videoUri = Uri.parse(withVideo)
        binding.videoView.setVideoURI(videoUri)
        binding.videoView.setOnPreparedListener { _: MediaPlayer ->
            //binding.videoView.start()
          //  Log.e(TAG, "initVideo: ready el video" )
            isReady = true
            if(currentMillis > 0 && SyncUsers.getInstance().getCurrentSession() != null && !SyncUsers.getInstance().getCurrentSession()!!.isPaused){
                playPauseVideo()
            }
        }
    }

    fun CardView.setText(str:String){
        val textV = getChildAt(0) as TextView

        textV.text = str
    }

    fun listener(session: SyncSession){

    }

    private var currentMillis = 0L
    private fun playPauseVideo() {
        if (isPlaying) {
            binding.videoView.pause()
            isPlaying = false
        } else {
            binding.videoView.seekTo(currentMillis.toInt())
            binding.videoView.start()

            // Iniciar el seguimiento de la posición del video
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if(!isPlaying) return
                    val milliseconds = binding.videoView.currentPosition.toLong()

                    if(SyncUsers.getInstance().isConnected() && SyncUsers.getInstance().isHost()){
                     //   Log.e(TAG, "run: $milliseconds" )
                        SyncUsers.getInstance().syncVideoState(milliseconds, !isPlaying)
                    }
                    // Repetir cada segundo
                    handler.postDelayed(this, 10000)
                }
            }, 10000)

            handlerTiming.postDelayed(object : Runnable {
                override fun run() {
                if(!isPlaying) return
                main{_ ->
                    val milliseconds = binding.videoView.currentPosition.toLong()
                    currentMillis = milliseconds
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                            TimeUnit.MINUTES.toSeconds(minutes)
                    binding.timestamp.text = String.format("%02d:%02d", minutes, seconds)

                }
                handlerTiming.postDelayed(this, 1000)
            } }, 1000)

            isPlaying = true
        }
    }
}