// Archivo: app/src/main/java/com/example/notifire/notifications/NotificationHistoryActivity.kt

package com.example.notifire.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notifire.R
import com.example.notifire.data.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationHistoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notifications = mutableListOf<Notification>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_history)

        listView = findViewById(R.id.listViewNotifications)

        // Configurar título de la actividad
        supportActionBar?.title = "Historial de Notificaciones"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar el adaptador personalizado
        adapter = NotificationAdapter(this, notifications)
        listView.adapter = adapter

        // Cargar notificaciones
        loadNotifications(user.uid)
    }

    private fun loadNotifications(uid: String) {
        db.collection("notifications")
            .document(uid)
            .collection("items")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                notifications.clear()
                for (doc in result) {
                    val title = doc.getString("title") ?: ""
                    val message = doc.getString("message") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                    notifications.add(Notification(title, message, timestamp))
                }
                adapter.notifyDataSetChanged()

                // Mostrar mensaje si no hay notificaciones
                if (notifications.isEmpty()) {
                    Toast.makeText(this, "No tienes notificaciones", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar notificaciones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Adaptador personalizado para mostrar las notificaciones
    inner class NotificationAdapter(
        context: android.content.Context,
        private val notifications: List<Notification>
    ) : ArrayAdapter<Notification>(context, 0, notifications) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var itemView = convertView
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(
                    R.layout.item_notification,
                    parent,
                    false
                )
            }

            val notification = notifications[position]

            val titleTextView = itemView?.findViewById<TextView>(R.id.textViewTitle)
            val messageTextView = itemView?.findViewById<TextView>(R.id.textViewMessage)
            val timeTextView = itemView?.findViewById<TextView>(R.id.textViewTime)

            titleTextView?.text = notification.title
            messageTextView?.text = notification.message
            timeTextView?.text = dateFormat.format(Date(notification.timestamp))

            return itemView!!
        }
    }
}