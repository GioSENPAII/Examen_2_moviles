// Archivo: app/src/main/java/com/example/notifire/common/SessionManager.kt

package com.example.notifire.common

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.notifire.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("notifire_session", Context.MODE_PRIVATE)
    private val KEY_UID = "uid"
    private val KEY_ROLE = "role"
    private val KEY_EMAIL = "email"
    private val KEY_NAME = "name"

    fun saveSession(uid: String) {
        prefs.edit().putString(KEY_UID, uid).apply()

        // Obtener y guardar datos adicionales del usuario
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role") ?: "user"
                    val email = document.getString("email") ?: ""
                    val name = document.getString("name") ?: ""

                    prefs.edit()
                        .putString(KEY_ROLE, role)
                        .putString(KEY_EMAIL, email)
                        .putString(KEY_NAME, name)
                        .apply()
                }
            }
    }

    fun getUid(): String? {
        return prefs.getString(KEY_UID, null)
    }

    fun getRole(): String {
        return prefs.getString(KEY_ROLE, "user") ?: "user"
    }

    fun getName(): String {
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    fun getEmail(): String {
        return prefs.getString(KEY_EMAIL, "") ?: ""
    }

    fun isAdmin(): Boolean {
        return getRole() == "admin"
    }

    fun isLoggedIn(): Boolean {
        return getUid() != null
    }

    fun logout() {
        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut()

        // Limpiar datos de sesión local
        prefs.edit().clear().apply()

        // Redirigir a login
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}