// Archivo: app/src/main/java/com/example/notifire/SplashActivity.kt

package com.example.notifire

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.notifire.auth.LoginActivity
import com.example.notifire.common.SessionManager
import com.example.notifire.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME: Long = 1500 // 1.5 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ocultar ActionBar en la pantalla de splash
        supportActionBar?.hide()

        // Verificar si hay sesión activa después de un breve retraso
        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
        }, SPLASH_TIME)
    }

    private fun checkSession() {
        val sessionManager = SessionManager(this)
        val auth = FirebaseAuth.getInstance()

        if (sessionManager.isLoggedIn() && auth.currentUser != null) {
            // Hay sesión activa, ir a Home
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // No hay sesión, ir a Login
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Cerrar esta actividad para que no se pueda volver atrás
        finish()
    }
}