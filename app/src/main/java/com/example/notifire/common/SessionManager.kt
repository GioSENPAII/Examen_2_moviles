package com.example.notifire.common

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("notifire_session", Context.MODE_PRIVATE)

    fun saveSession(uid: String) {
        prefs.edit().putString("uid", uid).apply()
    }

    fun getUid(): String? {
        return prefs.getString("uid", null)
    }

    fun isLoggedIn(): Boolean {
        return getUid() != null
    }

    fun logout(context: Context) {
        FirebaseAuth.getInstance().signOut()
        prefs.edit().clear().apply()
    }
}
