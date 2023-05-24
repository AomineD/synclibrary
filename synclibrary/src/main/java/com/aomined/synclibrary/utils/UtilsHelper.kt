package com.aomined.synclibrary.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aomined.synclibrary.R
import com.aomined.synclibrary.TAG
import com.aomined.synclibrary.data.SyncSession
import com.aomined.synclibrary.data.UserSession
import com.google.common.reflect.TypeToken
import com.google.firebase.firestore.DocumentSnapshot
import com.google.gson.Gson
import es.dmoral.toasty.Toasty
import java.util.*
import kotlin.reflect.full.memberProperties


object UtilsHelper {

    // Fill the App ID of your project generated on Agora Console.
    var appId = ""
    var appKeyChat = ""

    fun Activity.requestPermissionsSync(){
        if(checkSelfPermission()) return

        ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
    }



    const val PERMISSION_REQ_ID = 8340
    val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO
    )

    inline fun <reified T : Any> T.toMap(): Map<String, Any?> {
        val classInfo = T::class
        return classInfo.memberProperties.associate { prop ->
            prop.name to (prop.get(this) as? T)
        }
    }

    fun Context.checkSelfPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun String.getRoomCodeFrom(): String {
        val regex = "^jw-(.*)-room$".toRegex()
        val matchResult = regex.matchEntire(this)
        return matchResult?.groupValues?.getOrNull(1) ?: ""
    }

    fun String.isValidRoomCode(): Boolean {
        val regex = "^jw-.*-room$".toRegex()
        return this.matches(regex)
    }

    fun ImageView.hasDrawable(drawableResId: Int): Boolean {
        val context = this.context
        val expectedDrawable = ContextCompat.getDrawable(context, drawableResId)
        return compareBitmaps(drawableToBitmap(this.drawable), drawableToBitmap(expectedDrawable))
    }

    private fun compareBitmaps(b1: Bitmap?, b2: Bitmap?): Boolean {
        if (b1 == null || b2 == null) {
            return false
        }

        return b1.sameAs(b2)
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        // Si no es un BitmapDrawable, crea un bitmap vacío y dibuja el drawable en él
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    fun ImageView.setTint(colorResId: Int) {
        val colorStateList = ContextCompat.getColorStateList(context, colorResId)
        isSelected = false
        imageTintList = colorStateList ?: ColorStateList.valueOf(ContextCompat.getColor(context, colorResId))
        imageTintMode = PorterDuff.Mode.SRC_IN
    }

    fun Context.getDrawableCompat(res:Int):Drawable? = ContextCompat.getDrawable(this, res)

    fun showLog(message:String, log:Boolean = false){
        Log.e(TAG, "showLog: $message")
    }

    fun Context.hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Encuentra la vista actualmente enfocada, para que podamos tomar el token de ventana de ella.
        var view = (this as? Activity)?.currentFocus
        // Si no hay vista enfocada, crea una nueva para poder tomar un token de ventana de ella.
        if (view == null) {
            view = View(this)
        }
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun UUID.positiveHashCode(): Int {
        return this.hashCode() and Int.MAX_VALUE
    }

    fun RelativeLayout.getString(res: Int) = this.context.getString(res)

    fun CardView.setTextToChildTv(text: String) {
        val childCount = this.childCount
        for (i in 0 until childCount) {
            val child = this.getChildAt(i)
            if (child is TextView) {
                child.text = text
                break
            }
        }
    }

    fun SyncSession.toMap(): Map<String, Any> {
        val gson = Gson()
        val json = gson.toJson(this)
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(json, type)
    }

    fun DocumentSnapshot.toSyncSession(): SyncSession {
        val memberListSnapshot = get("memberList") as? List<HashMap<String, Any>> ?: emptyList()

        val memberList = memberListSnapshot.map { map ->
            UserSession(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                signUpMillis = map["signUpMillis"] as? Long,
                lastOnline = map["lastOnline"] as? Long ?: 0,
                uniqueId = map["uniqueId"] as? Int ?: -1,
                readyForPlay = map["readyForPlay"] as? Boolean ?: false
            )
        }

        return SyncSession(
            sessionId = getString("sessionId") ?: "",
            hostId = getString("hostId") ?: "",
            videoId = getString("videoId") ?: "",
            videoOption = getString("videoOption") ?: "",
            videoTimestamp = getLong("videoTimestamp") ?: 0,
            isPaused = getBoolean("isPaused") ?: true,
            lastOnlineHost = getLong("lastOnlineHost") ?: 0,
            timestamp = getLong("timestamp") ?: 0,
            memberList = memberList.toMutableList()
        )
    }

    fun SyncSession.isSync(currentTimeVideo: Long): Boolean {
        // Convertir la diferencia de tiempo a segundos
        val differenceInSeconds = (this.videoTimestamp - currentTimeVideo) / 1000
        return differenceInSeconds <= 6
    }

    fun Context.toastWarning(message:String){
        Toasty.warning(this, message).show()
    }

    fun Context.toastError(message:String){
        Toasty.error(this, message).show()
    }

    fun Context.inviteFriend(token: String) {
        val url = "https://playdomapp.top/party/$token"

        val compartirIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getString(R.string.w_invite_str, url))
            type = "text/plain"
        }

        startActivity(Intent.createChooser(compartirIntent, getString(R.string.w_sharevia)))
    }
}