package com.aomined.synclibrary.chat

import android.util.Log
import com.aomined.synclibrary.TAG
import com.aomined.synclibrary.UserStorage
import com.aomined.synclibrary.Watch2Application
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// ChatManager class
class ChatManager {

    companion object{
        private var instance:ChatManager? = null
        fun getInstance():ChatManager = instance ?: ChatManager()
    }

    init {
        instance = this
    }

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var chatCollectionRef:CollectionReference

    fun init(roomId: String, callback: (Boolean) -> Unit){
        val docRef = firestore.collection("chatRooms").document(roomId)
        docRef.set(HashMap<String, Any>())  // Crea el documento si no existe
            .addOnSuccessListener {
                chatCollectionRef = docRef.collection("messages")
                callback(true)
            }.addOnFailureListener {
                Log.e(TAG, "init: FAILED!!!!" )
                callback(false)
            }
    }

    fun sendMessage(chatMessage: ChatMessage, callback:(Boolean) -> Unit) {
        chatCollectionRef.add(chatMessage)
            .addOnSuccessListener {
                Log.d("ChatManager", "Mensaje enviado exitosamente: ${chatMessage.message}")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("ChatManager", "Error al enviar mensaje", exception)
                callback(false)
            }
    }

    private var chatListener:ListenerRegistration = ListenerRegistration { }
    fun addIncomingMessagesListener(listener: (ChatMessage) -> Unit) {
        chatListener = chatCollectionRef
            .orderBy("timestamp")
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) {
                    Log.e("ChatManager", "Error al escuchar mensajes entrantes", exception)
                    return@addSnapshotListener
                }

                querySnapshot?.documentChanges?.forEach { documentChange ->
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        val chatMessage = documentChange.document.toObject(ChatMessage::class.java)
                        listener(chatMessage)
                    }
                }
            }
    }

    fun buildMessage(messageContent: String): ChatMessage? {
        val user = UserStorage.getInstance(Watch2Application.instance).getUserSession()

        return if (user != null) {
            ChatMessage(
                userId = user.id,
                senderId = user.id,
                senderName = user.name,
                message = messageContent,
                timestamp = System.currentTimeMillis()
            )
        } else {
            null
        }
    }

    fun deleteAnyMineRoom(id:String){
        val roomID = "jw-${id}-room"
        Log.e(TAG, "deleteChatRoom: $roomID" )
        val ref = firestore.collection("chatRooms").document(roomID)
        chatListener.remove()
        ref.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.e(TAG, "Document exists!")

                    ref.delete()
                        .addOnCompleteListener {
                            if(it.isSuccessful){
                                Log.e(TAG, "se borro bien bro" )
                            }else{
                                Log.e(TAG, "No se borró bien bro: ${it.exception}" )
                            }
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "FAIL deleting: $it" )
                        }.addOnSuccessListener {
                            Log.e(TAG, "deletedChat: for $roomID" )
                        }

                } else {
                    Log.e(TAG, "No such document!")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check document", exception)
            }


    }
}