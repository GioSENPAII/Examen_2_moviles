// Archivo: app/src/main/java/com/example/notifire/auth/RegisterActivity.kt

package com.example.notifire.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.notifire.R
import com.example.notifire.common.SessionManager
import com.example.notifire.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class RegisterActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val name = findViewById<EditText>(R.id.editTextName)
        val email = findViewById<EditText>(R.id.editTextEmail)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val isAdminSwitch = findViewById<Switch>(R.id.switchAdmin)
        val masterKey = findViewById<EditText>(R.id.editTextMasterKey)
        val registerBtn = findViewById<Button>(R.id.buttonRegister)

        masterKey.isEnabled = false

        isAdminSwitch.setOnCheckedChangeListener { _, isChecked ->
            masterKey.isEnabled = isChecked
        }

        registerBtn.setOnClickListener {
            val nameStr = name.text.toString().trim()
            val emailStr = email.text.toString().trim()
            val passwordStr = password.text.toString().trim()
            val isAdmin = isAdminSwitch.isChecked
            val masterKeyStr = masterKey.text.toString().trim()

            if (nameStr.isEmpty() || emailStr.isEmpty() || passwordStr.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registerUser(
                nameStr,
                emailStr,
                passwordStr,
                isAdmin,
                masterKeyStr
            )
        }

        viewModel.authResult.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            if (it.contains("exitoso")) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    SessionManager(this).saveSession(uid)
                    // Actualizar token FCM
                    updateFCMToken(uid)

                    // Navegar a Home
                    startActivity(Intent(this, HomeActivity::class.java))
                }
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
                    // Suscribir al tema 'all_users' para notificaciones globales
                    FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                        .addOnSuccessListener {
                            Log.d("FCM", "Suscrito al tema 'all_users' correctamente")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FCM", "Error al suscribirse al tema: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Error al guardar token: ${e.message}")
                }
        }
    }
}