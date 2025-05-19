// Archivo: app/src/main/java/com/example/notifire/home/HomeActivity.kt

package com.example.notifire.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.notifire.R
import com.example.notifire.admin.NotificationPanelActivity
import com.example.notifire.common.SessionManager
import com.example.notifire.notifications.NotificationHistoryActivity
import com.example.notifire.profile.ProfileActivity

class HomeActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)

        val user = auth.currentUser
        if (user == null) {
            sessionManager.logout()
            return
        }

        // Configurar título de la aplicación
        supportActionBar?.title = "Notifire - Inicio"

        // Obtener referencias a vistas
        val welcomeText = findViewById<TextView>(R.id.textViewWelcome)
        val roleText = findViewById<TextView>(R.id.textViewRole)
        val sendNotificationsBtn = findViewById<Button>(R.id.buttonSendNotifications)
        val viewNotificationsBtn = findViewById<Button>(R.id.buttonViewNotifications)
        val profileBtn = findViewById<Button>(R.id.buttonProfile)
        val logoutBtn = findViewById<Button>(R.id.buttonLogout)

        // Verificar rol del usuario y configurar UI adecuadamente
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Usuario"
                val role = doc.getString("role") ?: "user"

                // Actualizar UI según el rol
                welcomeText.text = "¡Bienvenido, $name!"
                roleText.text = if (role == "admin") "Rol: Administrador" else "Rol: Usuario"

                // El botón de enviar notificaciones solo visible para administradores
                sendNotificationsBtn.visibility = if (role == "admin") View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
            }

        // Configurar listeners para botones
        sendNotificationsBtn.setOnClickListener {
            startActivity(Intent(this, NotificationPanelActivity::class.java))
        }

        viewNotificationsBtn.setOnClickListener {
            startActivity(Intent(this, NotificationHistoryActivity::class.java))
        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        logoutBtn.setOnClickListener {
            sessionManager.logout()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)

        // Solo mostrar opción de admin si el usuario es admin
        menu.findItem(R.id.action_admin_panel).isVisible = sessionManager.isAdmin()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_admin_panel -> {
                startActivity(Intent(this, NotificationPanelActivity::class.java))
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_notifications -> {
                startActivity(Intent(this, NotificationHistoryActivity::class.java))
                true
            }
            R.id.action_logout -> {
                sessionManager.logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}