// Archivo: app/src/main/java/com/example/notifire/auth/LoginActivity.kt

package com.example.notifire.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.notifire.R
import com.example.notifire.common.SessionManager
import com.example.notifire.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    // Solicitud de permisos para notificaciones en Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permisos de notificación concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Sin permisos de notificación, no recibirás alertas", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Solicitar permisos de notificación en Android 13+
        askNotificationPermission()

        val email = findViewById<EditText>(R.id.editTextEmail)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val loginBtn = findViewById<Button>(R.id.buttonLogin)

        loginBtn.setOnClickListener {
            val emailStr = email.text.toString().trim()
            val passwordStr = password.text.toString().trim()

            if (emailStr.isEmpty() || passwordStr.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(emailStr, passwordStr)
        }

        val textRegister = findViewById<TextView>(R.id.textRegister)

        textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        viewModel.authResult.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            if (it == "Login exitoso") {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    SessionManager(this).saveSession(uid)
                    // Actualizar token FCM
                    updateFCMToken(uid)
                }

                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        })
    }

    private fun updateFCMToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            // Obtiene el nuevo token
            val token = task.result

            // Guarda el token en Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("tokens").document(uid)
                .set(mapOf(
                    "token" to token,
                    "updatedAt" to System.currentTimeMillis()
                ))
                .addOnSuccessListener {
                    // Suscribir al tema 'all_users'
                    FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                }
        }
    }

    private fun askNotificationPermission() {
        // Verificar si estamos en Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
                // Permiso ya concedido
            } else {
                // Solicitar permiso
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}