package com.aomined.synclibrary

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aomined.synclibrary.agorautils.VoiceSync
import com.aomined.synclibrary.chat.ChatManager
import com.aomined.synclibrary.data.*
import com.aomined.synclibrary.listeners.SessionListener
import com.aomined.synclibrary.listeners.SyncSessionListener
import com.aomined.synclibrary.listeners.UserListener
import com.aomined.synclibrary.listeners.WatchListener
import com.aomined.synclibrary.utils.PopUpInit
import com.aomined.synclibrary.utils.UtilsHelper.getRoomCodeFrom
import com.aomined.synclibrary.utils.UtilsHelper.positiveHashCode
import com.aomined.synclibrary.utils.UtilsHelper.toMap
import com.aomined.synclibrary.utils.UtilsHelper.toSyncSession
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

const val TAG = "MAIN"


const val onlineThreshold = 30000L // 30 segundos

class SyncUsers {
    private val db = FirebaseFirestore.getInstance()
    private var watchListener:WatchListener = object : WatchListener{
        override fun onUserLogged() {

        }

        override fun onCreateSession(session: SyncSession) {

        }

        override fun onJoinedSession(session: SyncSession) {

        }
    }

    companion object{
        var baseUrl = "https://playdomapp.top/"
        private var isDebug = false
        fun setDebug(){ isDebug = true }
        private val instance = SyncUsers()
        fun getInstance() = instance
    }

    fun setSessionListener(sessionListener: SessionListener){
        this.outsideListener = sessionListener
    }

    fun setWatchListener(watchListener: WatchListener){
        this.watchListener = watchListener
    }

    fun isConnected() : Boolean = initialized && isConnectedToAnyParty
    fun isHost():Boolean = isHostInParty
    fun isInitialized():Boolean = initialized

    private var currentUserId = ""

    fun getMyUserId() = currentUserId

    private var initialized = false

    fun initSystem(context:Context, listener: UserListener){
        if(initialized) return


        currentUserId = UserStorage.getInstance(context).getUserId()

        val user = UserStorage.getInstance(context).getUserSession()

        user?.let {
            loginWithFirestore(user, {
                initializedFirst = true
                initialized = true
                UserStorage.getInstance(context).saveUserSession(it)
                listener.onSuccessLogin()
                watchListener.onUserLogged()
            }, {
                listener.onError(it)
            })
        } ?: run {
            val pop = PopUpInit(context)
            pop.showInit { usr ->
                initializedFirst = true
                val newUser = UserSession(currentUserId, usr)
                loginWithFirestore(newUser, {
                    pop.hideLoading()
                    pop.dismiss()
                    initialized = true
                    UserStorage.getInstance(context).saveUserSession(it)
                    listener.onSuccessLogin()
                    watchListener.onUserLogged()
                }, {
                    pop.hideLoading()
                    pop.showError(R.string.something_went_wrong)
                    listener.onError(it)
                })
            }

            pop.setOnDismissListener {
                if(!initializedFirst){
                    listener.onError(Exception("Dismissed"))
                }
            }
        }

    }

    var initializedFirst = false

    private val limitHostToDisconnect = 155000

    private var isConnectedToAnyParty = false
    private var isHostInParty = false

    fun getCurrentSession():SyncSession? = currentSessionIn

    private var outsideListener:SessionListener = object : SessionListener{
        override fun onUser(userEnter: Boolean, userId: String) {
            
        }

        override fun onSessionUpdated(updated: SyncSession) {
            
        }

        override fun onSessionError(error: String?) {
            
        }

        override fun onSessionDestroyed() {
            
        }
    }
    
    private var currentSessionIn:SyncSession? = null

    fun createSyncSession(
        hostSession: UserSession,
        videoId: String,
        videoOption:String,
        videoTimestamp: Long,
        listener: SyncSessionListener
    ) {

        if(!initialized){
            listener.onFailure(Exception("System was not initialized"))
            return
        }

        if(isHostInParty){
            listener.onFailure(Exception("Already in session"))
            return
        }

        if(isConnectedToAnyParty) {
            onDisconnectDevice()
        }

        val listMembers = mutableListOf(hostSession)
        val session = SyncSession(hostId = hostSession.id, videoId = videoId,
            videoOption = videoOption,
            videoTimestamp = videoTimestamp,
            lastOnlineHost = System.currentTimeMillis(),
            isPaused = true,
            timestamp = System.currentTimeMillis() / 1000,
            memberList = listMembers)

        if(isDebug) {
            Log.e(TAG, "createSyncSession: creating session...")
        }

        Log.e(TAG, "createSyncSession: ${session.toMap()}" )
        try {
            deleteAnyMineRoom{
                db.collection("syncSessions")
                    .add(session.toMap())
                    .addOnSuccessListener { documentReference ->
                        if(isDebug) {
                            Log.e(TAG, "createSyncSession: success")
                        }
                        isHostInParty = true
                        isConnectedToAnyParty = true
                        val sessionIdOf = "jw-${documentReference.id}-room"
                        db.collection("syncSessions")
                            .document(documentReference.id)
                            .update(mapOf("sessionId" to sessionIdOf))

                        currentSessionIn = session.copy(sessionId = sessionIdOf)
                        startMonitoringUsers()
                        listener.onSuccess(currentSessionIn)
                        VoiceSync.getInstance().joinChannel(sessionIdOf){
                            listener.onVoiceStateChanged(it)
                        }

                        watchListener.onCreateSession(currentSessionIn!!)
                        ChatManager.getInstance().init(sessionIdOf){
                                listener.onChatStateChanged(it)
                        }

                    }
                    .addOnFailureListener { e ->
                        listener.onFailure(e)
                    }
            }
        }catch (e:Exception){
            if(isDebug) {
                Log.e(TAG, "createSyncSessionException: ${e.message}")
            }
        }
    }

    val updateInterval = 20000L
    var handler:Handler? = null
    var updateRunnable:Runnable = object : Runnable {
        override fun run() {
            updateUserSession()
            handler?.postDelayed(this, updateInterval)
        }
    }

    private fun startMonitoringUsers() {
        handler = Handler(Looper.getMainLooper())
        handler?.post(updateRunnable)
    }

    private var listenerOnSession:ListenerRegistration? = null
    fun listenForSessionChanges(listener: SessionListener) {
        if(currentSessionIn == null || !isConnectedToAnyParty){
            listener.onSessionError("You're not in a room $currentSessionIn - $isConnectedToAnyParty")
            return
        }
        val sessionRef = db.collection("syncSessions").document(currentSessionIn!!.sessionId.getRoomCodeFrom())
        listenerOnSession = sessionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Si hay un error, notificar al listener
                outsideListener.onSessionError(error.message)
                listener.onSessionError(error.message)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Obtener los datos de la sesión
                Log.e(TAG, "listenForSessionChanges: ${snapshot.data}" )
                val session = snapshot.toSyncSession()

                // Verificar si la lista de miembros de la sesión no es nula y contiene más de un elemento
                if (session?.memberList != null && session.memberList.size > 1) {
                    // Obtener la lista de miembros
                    val memberIds = session.memberList.map { it.id }

                    // Verificar si el usuario actual es un miembro de la sesión
                    if (memberIds.contains(currentUserId)) {
                        // Obtener el ID del otro miembro (diferente al usuario actual)
                        val otherMemberId = memberIds.find { it != currentUserId }

                        // Verificar si el estado de la sesión ha cambiado
                        if (session.videoTimestamp != currentSessionIn?.videoTimestamp || session.isPaused != currentSessionIn?.isPaused) {
                            currentSessionIn = session
                           // listener.onSessionUpdated(session)
                        }

                        // Verificar si alguien se unió a la sesión
                        val currentGuestId = currentSessionIn?.memberList?.find { it.id != currentUserId }?.id
                        val newGuestId = session.memberList.find { it.id != currentUserId }?.id
                        if (newGuestId != null && newGuestId != currentGuestId) {
                            listener.onUser(true, newGuestId)
                        }

                        // Verificar si alguien salió de la sesión
                        val leftMemberId = currentSessionIn?.memberList?.find { it.id != currentUserId && !session.memberList.contains(it) }?.id
                        if (leftMemberId != null) {
                            outsideListener.onUser(false, leftMemberId)
                            listener.onUser(false, leftMemberId)
                        }

                        currentSessionIn = session
                    } else {
                        // Si el usuario no es un miembro de la sesión, notificar al listener
                        outsideListener.onUser(false, "")
                        listener.onUser(false, "")
                    }
                } else {
                    // Si la lista de miembros de la sesión es nula o solo contiene un elemento (host), notificar al listener
                    listener.onUser(false, "")
                    outsideListener.onUser(false, "")
                }
                session?.let {
                    currentSessionIn = session
                   // Log.e(TAG, "listenForSessionChanges: ${currentSessionIn!!.isPaused} and ${session.isPaused}" )
                    outsideListener.onSessionUpdated(session)
                    listener.onSessionUpdated(session)
                }
            }else{
                outsideListener.onSessionDestroyed()
                onDisconnectDevice()
                listener.onSessionDestroyed()
            }
        }
    }


    fun joinSyncSession(newMember: UserSession, sessionId: String, listener: SyncSessionListener) {
        val sessionRef = db.collection("syncSessions").document(sessionId.getRoomCodeFrom())

        sessionRef.get().addOnSuccessListener { docSnapshot ->
            if (docSnapshot.exists()) {
                val session = docSnapshot.toObject(SyncSession::class.java)

                if (session?.memberList != null) {
                    val currentMemberIds = session.memberList.map { it.id }
                    if (currentMemberIds.contains(newMember.id)) {

                        if(!session.hostIsOnline()){
                            onDisconnectDevice(true)
                            return@addOnSuccessListener
                        }

                        currentSessionIn = session
                        isConnectedToAnyParty = true
                        //setOtherListener(sessionRef, listener)
                        //Log.e(TAG, "joinSyncSession: esta success" )
                        listener.onSuccess(session)

                        VoiceSync.getInstance().joinChannel(sessionId){
                            Log.e("VOICE", "joinSyncSession response: $it" )
                            listener.onVoiceStateChanged(it)
                        }
                        watchListener.onJoinedSession(currentSessionIn!!)
                        Log.e(TAG, "createSyncSession: for chat " )
                        ChatManager.getInstance().init(sessionId){
                            listener.onChatStateChanged(it)
                        }




                        return@addOnSuccessListener
                    }

                    if (currentMemberIds.size >= 4) {
                        listener.onFailure(Exception("This session already has four members"))
                        return@addOnSuccessListener
                    }
                }

                if (session?.memberList != null && session.memberList.size < 4) {
                    // No hay un invitado en la sesión, o la sesión solo tiene al host
                    val updatedSession = session.copy(
                        memberList = session.memberList.apply { add(newMember) }
                    )

                    if(!session.hostIsOnline()){
                        onDisconnectDevice(true)
                        return@addOnSuccessListener
                    }

                    sessionRef.set(updatedSession).addOnSuccessListener {
                        currentSessionIn = updatedSession
                        isConnectedToAnyParty = true
                        startMonitoringUsers()
                        //setOtherListener(sessionRef, listener)
                        listener.onSuccess(updatedSession)

                        VoiceSync.getInstance().joinChannel(sessionId){
                            Log.e("VOICE", "joinSyncSession response: $it" )
                            listener.onVoiceStateChanged(it)
                        }
                        watchListener.onJoinedSession(currentSessionIn!!)
                        Log.e(TAG, "createSyncSession: for chat " )
                        ChatManager.getInstance().init(sessionId){
                            listener.onChatStateChanged(it)
                        }


                    }.addOnFailureListener { e ->
                        listener.onFailure(e)
                    }
                } else {
                    listener.onFailure(Exception("This session already has four members"))
                }
            } else {
                listener.onFailure(Exception("Session does not exist"))
            }
        }.addOnFailureListener { e ->
            listener.onFailure(e)
        }
    }


    fun maintainSession(){
        if(!isHostInParty || currentSessionIn == null) return

        db.collection("syncSession")
            .document(currentSessionIn!!.sessionId)
            .update(mapOf("lastOnlineHost" to System.currentTimeMillis()))
    }


    fun setImReady(isReady:Boolean, callback: (Boolean) -> Unit){
        if(!isConnected() || currentSessionIn == null) return

        currentSessionIn!!.memberList?.forEach{
            if(it.id == getMyUserId()){
                it.setReady(isReady)
                it.active()
            }
        }

        db.collection("syncSessions")
            .document(currentSessionIn!!.sessionId.getRoomCodeFrom())
            .update(mapOf(
                "memberList" to currentSessionIn!!.memberList
            )).addOnSuccessListener {
                callback(true)
                Log.e(TAG, "setImReady: isReady" )
            }.addOnFailureListener {
                 Log.e(TAG, "setImReady: $it" )
                callback(false)
            }
    }
    fun onDisconnectDevice(forceDelete:Boolean = false){

        if(!initialized){
            return
        }

        if(isConnectedToAnyParty && currentSessionIn != null){
            val sessionRef = db.collection("syncSessions").document(currentSessionIn!!.sessionId.getRoomCodeFrom())
            if(isHostInParty || forceDelete) {
                sessionRef.delete().addOnFailureListener {
                    Log.e(TAG, "error On Delete: ${it.message}" )
                }
            }else{

                currentSessionIn?.let { session ->
                    session.memberList?.let { members ->
                        val newMembers = members.filterNot { it.id == currentUserId }
                            // Utiliza una transacción para garantizar la integridad de los datos
                            db.runTransaction { transaction ->
                                transaction.update(sessionRef, "memberList", newMembers)
                                currentSessionIn = null
                            }.addOnSuccessListener {
                                Log.e(TAG, "Miembro eliminado y actualizado con éxito.")
                            }.addOnFailureListener { e ->
                                Log.e(TAG, "Error al eliminar y actualizar miembro.", e)
                            }
                    } ?: run {
                        Log.e(TAG, "memberList es nulo.")
                    }
                } ?: run {
                    Log.e(TAG, "currentSessionIn es nulo.")
                }

            }
            ChatManager.getInstance().deleteAnyMineRoom(currentSessionIn!!.sessionId.getRoomCodeFrom())
            isConnectedToAnyParty = false
            isHostInParty = false
        }

        listenerOnSession?.remove()
        Log.e(TAG, "onDisconnectDevice: ups disconnected" )
        handler?.removeCallbacks(updateRunnable)
        VoiceSync.getInstance().leaveChannel()
    }


    // Función para comenzar la sincronización
    fun syncVideoState(videoTime:Long, isPaused:Boolean) {

        if(!initialized || !isHost()){
            return
        }


        Log.e(TAG, "syncVideoState: sync pause $isPaused" )
        val hostOnline = System.currentTimeMillis()

        currentSessionIn = currentSessionIn!!.copy(
            videoTimestamp = videoTime,
            isPaused = isPaused,
            lastOnlineHost = hostOnline
        )

        db.collection("syncSessions")
            .document(currentSessionIn!!.sessionId.getRoomCodeFrom())
            .update(mapOf(
                "videoTimestamp" to videoTime,
                "isPaused" to isPaused,
                "lastOnlineHost" to hostOnline
            )).addOnSuccessListener {

                Log.e(TAG, "syncVideoState: actualizado" )
            }.addOnFailureListener {
               // Log.e(TAG, "syncVideoState: $it" )
            }
    }

    private fun loginWithFirestore(
        userSession: UserSession,
        onSuccess: (UserSession) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        // Consulta si el usuario ya existe en Firestore
        db.collection("users")
            .document(userSession.id)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val user = if (documentSnapshot.exists()) {
                    // Si el usuario existe, devuelve la sesión guardada en Firestore
                    documentSnapshot.toObject(UserSession::class.java)!!
                } else {
                    // Si el usuario no existe, crea una nueva sesión en Firestore
                    val user = UserSession(userSession.id, userSession.name, System.currentTimeMillis(), uniqueId = UUID.randomUUID().positiveHashCode() )
                    db.collection("users")
                        .document(userSession.id)
                        .set(user)
                        .addOnSuccessListener {
                            Log.d("Firestore", "User session created for user ${userSession.id}")
                        }
                        .addOnFailureListener(onFailure)
                    user
                }
                onSuccess(user)
            }
            .addOnFailureListener(onFailure)
    }


    fun deleteRoom(callback:(Boolean) -> Unit){
        if(!isConnected() || !isHost() || currentSessionIn == null){
            callback(false)
            return
        }

        val sessionId = currentSessionIn!!.sessionId
        val sessionRef = db.collection("syncSessions").document(sessionId.getRoomCodeFrom())

        sessionRef.delete()
            .addOnSuccessListener {
                currentSessionIn = null
                isConnectedToAnyParty = false
                isHostInParty = false
                callback(true)
                VoiceSync.getInstance().leaveChannel()
                ChatManager.getInstance()
                    .deleteAnyMineRoom(sessionId.getRoomCodeFrom())

            }.addOnFailureListener {
                Log.e(TAG, "deleteRoom: ${it.message}" )
                callback(false)
            }
    }

    private fun deleteAnyMineRoom(callback: () -> Unit) {
        val sessionRef = db.collection("syncSessions")

        // Consulta para obtener documentos con hostId igual a currentUserId
        sessionRef.whereEqualTo("hostId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val size = documents.size()
                var counting = 0

                // Si no hay documentos que coincidan, ejecuta el callback inmediatamente
                if (size == 0) {
                    callback()
                } else {
                    for (document in documents) {
                        // Elimina cada documento que coincida

                        sessionRef.document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                counting++
                                if (counting == size) {
                                    callback()
                                }
                                Log.e("Firestore", "DocumentSnapshot successfully deleted!")
                                ChatManager.getInstance().deleteAnyMineRoom(document.id)
                            }
                            .addOnFailureListener { exception ->
                                counting++
                                if (counting == size) {
                                    callback()
                                }
                                Log.e("Firestore", "Error deleting document", exception)
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                callback()
                Log.e("Firestore", "Error getting documents: ", exception)
            }
    }

    fun updateUserSession() {
        currentSessionIn?.let { session ->
            val userSessionRef = db.collection("syncSessions").document(session.sessionId.getRoomCodeFrom())
            val userId = currentUserId // Reemplaza esto con el ID de usuario actual
            val updatedMemberList = session.memberList?.map { userSession ->
                if (userSession.id == userId) {
                    userSession.copy(lastOnline = System.currentTimeMillis())
                } else {
                    userSession
                }
            }

            userSessionRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {

                        userSessionRef.update("memberList", updatedMemberList)
                            .addOnSuccessListener {
                                Log.e("Firestore", "UserSession lastOnline updated successfully")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Error updating UserSession lastOnline", exception)
                            }

                    }else{
                        onDisconnectDevice()
                    }
                }
                .addOnFailureListener { exception ->
                    // Error al obtener el documento
                    Log.d("Firestore", "Error al obtener el documento: ", exception)
                }

        }
    }

    fun removeMember(pos:Int, callback: (Boolean) -> Unit){
        currentSessionIn?.let {
            val members = it.memberList
            members?.let { list ->
                list.removeAt(pos)
                val userSessionRef = db.collection("syncSessions").document(it.sessionId.getRoomCodeFrom())
                userSessionRef.update("memberList", list)
                    .addOnSuccessListener {
                        callback(true)
                    }.addOnFailureListener {
                        callback(false)
                    }
            }
        }
    }

    fun logOut(){
        onDisconnectDevice()
        currentUserId = ""
        initialized = false
        UserStorage.getInstance().clearData()
    }
}