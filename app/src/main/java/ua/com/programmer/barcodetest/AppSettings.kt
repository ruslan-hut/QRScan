package ua.com.programmer.qrscanner

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import java.lang.Exception
import java.util.*

class AppSettings(private val context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val tag = "ua.com.programmer.qrscanner.preference"
        sharedPreferences = context.getSharedPreferences(tag, Context.MODE_PRIVATE)
    }

    fun launchCounter(): Int {
        val counterTag = "APP_START_COUNTER"
        var value = sharedPreferences.getInt(counterTag, 0)
        value++
        val editor = sharedPreferences.edit()
        editor.putInt(counterTag, value)
        editor.apply()
        return value
    }

    fun userID(): String {
        val userIdTag = "USER_ID"
        var userID = sharedPreferences.getString(userIdTag, null)
        if (userID == null) {
            userID = UUID.randomUUID().toString()
            val time = Calendar.getInstance().timeInMillis
            userID = "$userID-$time"
            val editor = sharedPreferences.edit()
            editor.putString(userIdTag, userID)
            editor.apply()
        }
        return userID
    }

    fun versionName(): String {
        return BuildConfig.VERSION_NAME
    }

    fun firestorePassword(): String? {
        try {
            val applicationInfo = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val bundle = applicationInfo.metaData
            return bundle.getString("ua.com.programmer.qrscanner.default_user_pass")
        } catch (e: Exception) {
            Log.e("XBUG", "meta-data: $e")
        }
        return null
    }

}