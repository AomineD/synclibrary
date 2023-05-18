package com.aomined.synclibrary.chat
import android.content.Context
import com.aomined.synclibrary.R
import com.aomined.synclibrary.UserStorage
import com.aomined.synclibrary.Watch2Application
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ChatMessage data class con campo userId añadido
data class ChatMessage(
    val userId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0
)

// Función de extensión para obtener la fecha y hora legible
fun ChatMessage.formattedDateTime(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun ChatMessage.isMine():Boolean {
    val myUserId = UserStorage.getInstance(Watch2Application.instance).getUserId()
    if(myUserId.isEmpty()){
        return false
    }

    return myUserId == userId
}

fun ChatMessage.formattedDateTime(context: Context): String {
    val now = Date().time
    val diff = now - timestamp

    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        seconds < 60 -> context.resources.getQuantityString(R.plurals.time_seconds_ago, seconds.toInt(), seconds)
        minutes < 60 -> context.resources.getQuantityString(R.plurals.time_minutes_ago, minutes.toInt(), minutes)
        hours < 24 -> context.resources.getQuantityString(R.plurals.time_hours_ago, hours.toInt(), hours)
        days == 1L -> context.getString(R.string.time_yesterday)
        days in 2..30 -> context.resources.getQuantityString(R.plurals.time_days_ago, days.toInt(), days)
        else -> context.getString(R.string.time_long_ago)
    }
}