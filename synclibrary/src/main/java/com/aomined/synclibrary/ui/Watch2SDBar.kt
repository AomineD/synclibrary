package com.aomined.synclibrary.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import com.aomined.synclibrary.R
import com.aomined.synclibrary.SyncUsers
import com.aomined.synclibrary.TAG
import com.aomined.synclibrary.UserStorage
import com.aomined.synclibrary.agorautils.VoiceSync
import com.aomined.synclibrary.chat.ChatManager
import com.aomined.synclibrary.chat.ChatMessage
import com.aomined.synclibrary.data.*
import com.aomined.synclibrary.databinding.SiDEBaRBinding
import com.aomined.synclibrary.enum.ViewType
import com.aomined.synclibrary.listeners.SessionListener
import com.aomined.synclibrary.listeners.SyncSessionListener
import com.aomined.synclibrary.ui.adapter.MessageChatAdapter
import com.aomined.synclibrary.ui.adapter.UserSessionAdapter
import com.aomined.synclibrary.ui.viewmodel.SyncViewModel
import com.aomined.synclibrary.utils.CoroutineHelper.ioSafe
import com.aomined.synclibrary.utils.CoroutineHelper.main
import com.aomined.synclibrary.utils.UtilsHelper.getDrawableCompat
import com.aomined.synclibrary.utils.UtilsHelper.getString
import com.aomined.synclibrary.utils.UtilsHelper.hasDrawable
import com.aomined.synclibrary.utils.UtilsHelper.hideKeyboard
import com.aomined.synclibrary.utils.UtilsHelper.inviteFriend
import com.aomined.synclibrary.utils.UtilsHelper.isValidRoomCode
import com.aomined.synclibrary.utils.UtilsHelper.setTextToChildTv
import com.aomined.synclibrary.utils.UtilsHelper.setTint
import dagger.hilt.android.AndroidEntryPoint
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.delay

@AndroidEntryPoint
class Watch2SDBar(context: Context):RelativeLayout(context) {
    private var binding:SiDEBaRBinding
    private var viewType:ViewType = ViewType.NONE
    private var user:UserSession? = null
    private lateinit var watch2GetterButton: Watch2GetterButton

    private var videoId = ""
    private var videoOption = ""

    private var viewModel: SyncViewModel

    companion object{
       private var listener:SessionListener = object : SessionListener{
           override fun onUser(userEnter: Boolean, userId: String) {

           }

           override fun onSessionUpdated(updated: SyncSession) {

           }

           override fun onSessionError(error: String?) {

           }

           override fun onSessionDestroyed() {

           }
       }

        fun setSessionListenerOn(listenerSession:SessionListener){
            listener = listenerSession
        }
    }

    private fun getJWRoomFromClipboard(): String {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboardManager.primaryClip?.getItemAt(0)
        val clipboardText = item?.text?.toString()?.trim() ?: ""
        val regex = "^jw-.*-room$".toRegex()

        return if (clipboardText.matches(regex)) {
            clipboardText
        } else {
            ""
        }
    }

    fun setWatchButton(watch2GetterButton: Watch2GetterButton){
        this.watch2GetterButton = watch2GetterButton
    }

    fun setVideoConfig(videoId:String, videoOption:String = ""){
        this.videoId = videoId
        this.videoOption = videoOption
    }

    init {
        viewModel = ViewModelProvider(context as ViewModelStoreOwner)[SyncViewModel::class.java]
        VoiceSync.getInstance().setViewModel(viewModel)
        val inflater = LayoutInflater.from(context)
        binding = SiDEBaRBinding.inflate(inflater, this, true)
        configClicks()
        user = UserStorage.getInstance(context).getUserSession()
    }

    private fun setParentWidth() {
        val width = when(viewType){
            ViewType.NONE -> resources.getDimensionPixelSize(R.dimen.none_width)
            ViewType.CREATE -> resources.getDimensionPixelSize(R.dimen.create_width)
            ViewType.JOIN -> resources.getDimensionPixelSize(R.dimen.create_width)
            ViewType.CHAT -> resources.getDimensionPixelSize(R.dimen.create_width)
        }

        Log.e(TAG, "setParentWidth: ${viewType.name} - $width - ${binding.backgroundParent.width}" )
        val currentWidth = binding.backgroundParent.width
        val valueAnimator = ValueAnimator.ofInt(currentWidth, width)
        valueAnimator.duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        valueAnimator.addUpdateListener { animator ->
            val value = animator.animatedValue as Int
            binding.backgroundParent.layoutParams.width = value
            binding.backgroundParent.requestLayout()
        }

        valueAnimator.start()
    }

    fun joinInRoom(roomCode:String){
        binding.btnJoin.performClick()
        binding.editTextRoomCode.setText(roomCode)
        binding.joinRoomBtn.performClick()
    }
    private fun configClicks() = with(binding){
        btnJoin.setOnClickListener {
            resetBtns()
            it.isSelected = true
            joinRoom()
        }

        btnCreate.setOnClickListener {
            resetBtns()
            it.isSelected = true
            createRoom()
        }

        btnChatText.setOnClickListener {
            resetBtns()
            it.isSelected = true
            openChat()
        }

        btnLogOut.setOnClickListener {
            SyncUsers.getInstance().logOut()
            listMessage.clear()
            memberList.clear()
            abandonedRoom()
            watch2GetterButton.hideSdBarAnim()
            watch2GetterButton.setIconState()

        }

        offScreenSd.setOnClickListener {
            watch2GetterButton.hideSdBarAnim()
        }
    }

    private fun openChat() = with(binding) {
        resetMenus(false)
        viewType = ViewType.CHAT
        setParentWidth()
        chatMenu.isVisible = true
        chatLayout.isVisible = true
    }

    private var isLoadingJoin = false
    private fun joinRoom() = with(binding){
        if(SyncUsers.getInstance().isConnected()){
            return@with
        }

        isLoadingJoin = false
        joinLoading.isVisible = false

        resetMenus(false)
        viewType = ViewType.JOIN
        setParentWidth()
        joinMenu.isVisible = true
        val getFromCopy = getJWRoomFromClipboard()

        if(getFromCopy.isNotEmpty()){
            editTextRoomCode.setText(getFromCopy)
        }

        joinRoomBtn.setOnClickListener {

            val roomCode = editTextRoomCode.text.toString()

            if(roomCode.isEmpty()){
                editTextRoomCode.error = context.getString(R.string.w_please_put_code)
                return@setOnClickListener
            }

            if(!roomCode.isValidRoomCode()){
                editTextRoomCode.error = context.getString(R.string.w_invalid_roomcode)
                return@setOnClickListener
            }

            if(isLoadingJoin){
                return@setOnClickListener
            }
            isLoadingJoin = true
            joinLoading.isVisible = true

            //Log.e(TAG, "joinRoom: joining again" )

            user?.let {
                SyncUsers.getInstance()
                    .joinSyncSession(it, roomCode, object:SyncSessionListener{
                        override fun onSuccess(session: SyncSession?) {
                            Log.e(TAG, "onSuccess: ${session?.sessionId}" )
                            main{
                                editTextRoomCode.setText("")
                                isLoadingJoin = false
                                joinLoading.isVisible = false
                            }
                            session?.let { it1 -> createdOrJoinedSuccess(it1, false) }
                        }

                        override fun onFailure(e: Exception) {
                            Log.e(TAG, "onFailure: $e" )
                            e.message?.let { it1 -> setErrorJoin(it1) }
                        }

                        override fun onSessionTerminated() {

                        }

                        override fun onVoiceStateChanged(syncResponse: SyncResponse) {
                            Log.e(TAG, "onVoiceStateChanged: ${syncResponse.status}" )
                            when(syncResponse.status){
                                StatusSync.NEW_USER -> {

                                }
                                StatusSync.ERROR -> {
                                    enableMicFunctions(false)
                                }
                                StatusSync.DISCONNECTED -> {
                                    enableMicFunctions(false)

                                }
                                StatusSync.OK -> {
                                    enableMicFunctions(true)
                                    loadingStateInVoice(false)
                                }
                                StatusSync.LOADING -> {
                                    loadingStateInVoice(true)
                                }
                            }
                        }

                        override fun onNotice(type: String) {

                        }

                        override fun onChatStateChanged(isSuccess: Boolean) {
                            if(isSuccess) {
                                startListener()
                            }
                        }

                    })
            } ?: run {
                setErrorJoin("Your user is wrong")
            }
        }
    }

    private fun enterInRoom(entering:Boolean = true) = with(binding){
        main {
            joinLoading.isVisible = !entering
            joinLayout.isVisible = !entering
            createMenu.isVisible = entering
            btnJoin.isVisible = !entering
            val iconBtn = ContextCompat.getDrawable(context, if(entering) R.drawable.ro_oom_ic_on else R.drawable.cre_ate_btn)
            val text = context.getString(if(entering) R.string.w_inroom else R.string.w_createbtn)
            textCreateRoom.text = text
            iconCreateRoom.setImageDrawable(iconBtn)
            loadingLayout.isVisible = !entering
        }
    }

    private fun setErrorJoin(error:String) = with(binding){
        main {
            animInJoin.setAnimation(context.getString(R.string.error_anim))
            animInJoin.repeatCount = 0
            animInJoin.playAnimation()

            textLoadingJoin.text = error
            joinLoading.isVisible = true
            delay(3500)
            joinLoading.isVisible = false
            resetLoadingJoin()
        }
    }

    private fun resetLoadingJoin() = with(binding){
        main{
            animInJoin.repeatCount = LottieDrawable.INFINITE
            animInJoin.playAnimation()
            animInJoin.setAnimation(context.getString(R.string.loading_sync_anim))
            textLoadingJoin.setText(R.string.w_joining_room)
        }
    }

    private fun createRoom() = with(binding){
        if(videoId.isEmpty()){
            Toasty.warning(context, context.getString(R.string.w_noid_video)).show()
            return@with
        }
        resetMenus(false)
        viewType = ViewType.CREATE
        setParentWidth()
        createMenu.isVisible = true

        //creating room
        if(SyncUsers.getInstance().isConnected()){

            return@with
        }

        val userSession = UserStorage.getInstance(context).getUserSession()
        userSession?.let {
            loadingLayout.isVisible = true
            SyncUsers.getInstance().createSyncSession(it, videoId, videoOption,0, object:SyncSessionListener{
                override fun onSuccess(session: SyncSession?) {
                    loadingLayout.isVisible = false
                    session?.let {
                        createdOrJoinedSuccess(session)
                    }
                }

                override fun onFailure(e: Exception) {
                    loadingLayout.isVisible = false
                }

                override fun onSessionTerminated() {

                }

                override fun onVoiceStateChanged(syncResponse: SyncResponse) {
                    when(syncResponse.status){
                        StatusSync.NEW_USER -> {

                        }
                        StatusSync.ERROR -> {
                            enableMicFunctions(false)
                        }
                        StatusSync.DISCONNECTED -> {
                             enableMicFunctions(false)
                        }
                        StatusSync.OK -> {
                             enableMicFunctions(true)
                            loadingStateInVoice(false)
                        }
                        StatusSync.LOADING -> {
                            loadingStateInVoice(true)
                        }
                    }
                }

                override fun onNotice(type: String) {

                }

                override fun onChatStateChanged(isSuccess: Boolean) {
                    if(isSuccess) {
                        startListener()
                    }
                }

            })
        }

    }




    private var adapter:UserSessionAdapter? = null
    private var memberList = mutableListOf<UserSession>()

    fun createdOrJoinedSuccess(session: SyncSession, created:Boolean = true) = with(binding){
        enterInRoom()
        syncRoomIfNot(session)
        if(created){
            deleteRoomBtn.setOnClickListener {
                setLoadingInRoom(true, R.string.w_deleting_room)
                SyncUsers.getInstance().deleteRoom {
                    abandonedRoom()
                  //  Log.e(TAG, "createdOrJoinedSuccess: borrado? $it" )
                }
            }

            deleteRoomBtn.setTextToChildTv(getString(R.string.w_delete_room))

        }else{
            deleteRoomBtn.setTextToChildTv(getString(R.string.w_leave_room))
            deleteRoomBtn.setOnClickListener {
                SyncUsers.getInstance().onDisconnectDevice()
                roomLayout.isVisible = false
                setLoadingInRoom(true, R.string.w_exit_from_room, R.string.warn_anim, false)
                ioSafe {
                    delay(2000)
                    //Log.e(TAG, "syncRoomIfNot: ejecuta ahora" )
                    main{
                        //  setLoadingInRoom(false, R.string.w_creating_room)
                        abandonedRoom()
                    }
                }
            }
        }


        inviteRoomBtn.setOnClickListener {
            val code = session.sessionId
            /*val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", code)
            clipboard.setPrimaryClip(clip)
            inviteTv.setText(R.string.w_code_copied)
            inviteRoomBtn.isEnabled = false*/
            context.inviteFriend(code)
        }

        SyncUsers.getInstance()
            .listenForSessionChanges(object:SessionListener{
                override fun onUser(userEnter: Boolean, userId: String) {
                    listener.onUser(userEnter, userId)
                }

                override fun onSessionUpdated(updated: SyncSession) {
                   // Log.e(TAG, "onSessionUpdated: updated!" )
                    syncRoomIfNot(updated)
                    listener.onSessionUpdated(updated)
                }

                override fun onSessionError(error: String?) {
                    Log.e(TAG, "onSessionError: session error $error" )
                    listener.onSessionError(error)
                }

                override fun onSessionDestroyed() {
                    if(SyncUsers.getInstance().isHost() || !SyncUsers.getInstance().isConnected()) return

                    main {
                        roomLayout.isVisible = false
                    }
                    setLoadingInRoom(true, R.string.w_host_disconnected, R.string.warn_anim, false)
                    ioSafe {
                        delay(2000)
                        //Log.e(TAG, "syncRoomIfNot: ejecuta ahora" )
                        main{
                            //  setLoadingInRoom(false, R.string.w_creating_room)
                            abandonedRoom()
                        }
                    }
                    listener.onSessionDestroyed()
                }
            })
    }

    private fun syncRoomIfNot(session: SyncSession) = with(binding){

            if(!session.hostIsOnline() && !SyncUsers.getInstance().isHost()){
                SyncUsers.getInstance().onDisconnectDevice(false)
                roomLayout.isVisible = false
                setLoadingInRoom(true, R.string.w_host_disconnected, R.string.warn_anim, false)
                ioSafe {
                    delay(2000)
                    main{
                        //  setLoadingInRoom(false, R.string.w_creating_room)
                        abandonedRoom()
                    }
                }
                return@with
            }

            if(!session.iamInThisRoom()){
                SyncUsers.getInstance().onDisconnectDevice(false)
                roomLayout.isVisible = false
                setLoadingInRoom(true, R.string.w_i_removed_user, R.string.warn_anim, false)
                ioSafe {
                    delay(2000)
                    main{
                      //  setLoadingInRoom(false, R.string.w_creating_room)
                        abandonedRoom()
                    }
                }
                return@with
            }

            roomLayout.isVisible = true
            session.memberList?.let {
                memberList.clear()
                memberList.addAll(session.memberList)
                if(recMembers.adapter == null){
                    adapter = UserSessionAdapter(memberList, user?.id == session.hostId, session.hostId){
                        main {_ ->
                            setLoadingInRoom(true, R.string.w_expulsing_member)
                            SyncUsers.getInstance().removeMember(it){ result ->
                                if(!result){

                                }else{
                                    setLoadingInRoom(false, R.string.w_expulsing_member)
                                    resetLoadingMenuCreation()
                                }
                            }
                        }
                    }
                    recMembers.layoutManager = LinearLayoutManager(context)
                    recMembers.adapter = adapter
                }else{
                    adapter?.notifyDataSetChanged()
                }
            }
    }

    // UI functions

    private fun resetMenus(needCalculateWidth:Boolean = true, clearSelected:Boolean = false) = with(binding){
        viewType = ViewType.NONE
        joinMenu.isVisible = false
        createMenu.isVisible = false
        chatMenu.isVisible = false

        if(clearSelected) {
            btnCreate.isSelected = false
            btnJoin.isSelected = false
            btnChatText.isSelected = false
        }
        if(needCalculateWidth){
            setParentWidth()
        }
    }

    private fun resetBtns() = with(binding){
        btnJoin.isSelected = false
        btnCreate.isSelected = false
        btnLogOut.isSelected = false
        btnChatText.isSelected = false
    }

    private fun deactivePrincipal() = with(binding){
        btnJoin.isEnabled = false
        btnCreate.isEnabled = false
        btnLogOut.isEnabled = false
    }

    private fun setLoadingInRoom(loading:Boolean, res:Int, assetLottie:Int = R.string.loading_sync_anim, loop:Boolean = true) = with(binding){
        main {
            roomLayout.isVisible = !loading
            loadingLayout.isVisible = loading
            animOnCreate.repeatCount = if(loop) LottieDrawable.INFINITE else 0
            animOnCreate.setAnimation(context.getString(assetLottie))
            animOnCreate.playAnimation()
            changeTextLoadingTemporal(res)
        }
    }

    private fun changeTextLoadingTemporal(res:Int) = with(binding){
        main {
            textLoadingCreate.text = context.getString(res)
        }
    }

    private fun resetLoadingMenuCreation() = with(binding){
        main {
            textLoadingCreate.setText(R.string.w_creating_room)
            animOnCreate.repeatCount = LottieDrawable.INFINITE
            animOnCreate.setAnimation(context.getString(R.string.loading_sync_anim))
            animOnCreate.playAnimation()
        }
    }

    private fun abandonedRoom() = with(binding){
        resetLoadingMenuCreation()
        enterInRoom(false)
        //Log.e(TAG, "abandonedRoom: calling resetMenus" )
        resetMenus(needCalculateWidth = true, clearSelected = true)
        isLoadingJoin = false
        joinLoading.isVisible = false
        chatMenu.isVisible = false
        btnChatText.isVisible = false
        recMembers.adapter = null
    }

    private fun enableMicFunctions(visibleMicBtn:Boolean){
        main {
            binding.btnMute.isVisible = visibleMicBtn

            if(visibleMicBtn){
                binding.btnMute.setOnClickListener {
                    val currentState = binding.micIcon.hasDrawable(R.drawable.w_un_m_ut_e)
                    VoiceSync.getInstance().muteOrUnmute(currentState)
                    binding.micIcon.setImageDrawable(context.getDrawableCompat(if(currentState) R.drawable.w_m_ut_e else R.drawable.w_un_m_ut_e))
                    binding.micTextView.setText(if(currentState) R.string.w_unmute_text else R.string.w_mute_text)
                    //Log.e(TAG, "enableMicFunctions: va a ser rojo $currentState" )
                    binding.micIcon.setTint( if(currentState) R.color.w_redcolor else R.color.text_btn_color)
                    //binding.ratioMic.isVisible = !currentState
                }
            }else{
                binding.btnMute.setOnClickListener {

                }
            }
        }
    }

    private fun loadingStateInVoice(loading:Boolean){
        main {
            binding.btnMute.isVisible = true
            binding.voiceLoadingAnim.isVisible = loading
            binding.micIcon.isVisible = !loading
            val currentState = !binding.micIcon.hasDrawable(R.drawable.w_un_m_ut_e)
            val noLoadingText = if(currentState) R.string.w_unmute_text else R.string.w_mute_text
            binding.micTextView.setText(if(loading) R.string.w_loading_voice_sync else noLoadingText)
            binding.ratioMic.isVisible = false
        }
    }

    //CHAT

    private val listMessage = mutableListOf<ChatMessage>()
    private var adapterMessage:MessageChatAdapter? = null
    private fun startListener() {

        ChatManager.getInstance().addIncomingMessagesListener {
            manageMessage(it)
        }

        main {
                listMessage.clear()
                binding.recMessages.layoutManager = LinearLayoutManager(context)
                adapterMessage = MessageChatAdapter(listMessage)
                binding.recMessages.adapter = adapterMessage
                binding.btnChatText.isVisible = true
                binding.sendChatBtn.setOnClickListener {
                    val messageTo = binding.editTextMessages.text.toString()
                    if(messageTo.isEmpty()){
                        binding.editTextMessages.error = context.getString(R.string.w_empty_message)
                        return@setOnClickListener
                    }

                    val message = ChatManager.getInstance().buildMessage(messageTo)

                    if(message == null){
                        binding.editTextMessages.error = context.getString(R.string.w_error_null_message)
                        return@setOnClickListener
                    }

                    binding.textBtnSend.setText(R.string.w_sending_message)
                    binding.sendChatBtn.isEnabled = false
                    binding.editTextMessages.setText("")
                    binding.editTextMessages.clearFocus()
                    context.hideKeyboard()

                    ChatManager.getInstance().sendMessage(message){
                        main { _ ->
                            if(!it){
                                Toasty.warning(context, R.string.w_error_message_sending).show()
                            }
                            binding.textBtnSend.setText(R.string.w_send_chatmessage)
                            binding.sendChatBtn.isEnabled = true
                        }
                    }
                }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun manageMessage(message: ChatMessage) {
        main {
            if(binding.emptyChat.isVisible){
                binding.emptyChat.isVisible = false
                binding.recMessages.isVisible = true
            }
            listMessage.add(message)
            adapterMessage?.notifyDataSetChanged()
        }
    }
}