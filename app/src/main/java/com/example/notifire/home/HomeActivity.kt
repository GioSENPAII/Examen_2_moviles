package com.example.notifire.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.notifire.admin.NotificationPanelActivity
import com.example.notifire.profile.ProfileActivity
import com.example.notifire.notifications.NotificationHistoryActivity

class HomeActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser

        if (user == null) {
            finish() // Por seguridad, si no hay sesión válida
            return
        }

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")

                when (role) {
                    "admin" -> {
                        startActivity(Intent(this, NotificationPanelActivity::class.java))
                        finish()
                    }
                    "user" -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        finish()
                    }
                    else -> {
                        Toast.makeText(this, "Rol no definido", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener datos", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}
