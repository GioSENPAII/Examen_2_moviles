// Archivo: app/src/main/java/com/example/notifire/admin/LocalNotificationManager.kt

package com.example.notifire.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notifire.R
import com.example.notifire.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Random

class LocalNotificationManager(private val context: Context) {
    private val channelId = "notifire_channel"
    private val db = FirebaseFirestore.getInstance()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifire Notificaciones"
            val descriptionText = "Canal para notificaciones de Notifire"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // En LocalNotificationManager.kt:

    fun sendLocalNotification(title: String, message: String) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationId = Random().nextInt(1000)
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d("LocalNotification", "Notificación local mostrada: $title - $message")

            // Guardar también en la base de datos del usuario actual
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .document(currentUser.uid)
                    .collection("items")
                    .add(mapOf(
                        "title" to title,
                        "message" to message,
                        "timestamp" to System.currentTimeMillis()
                    ))
            }
        } catch (e: SecurityException) {
            Log.e("LocalNotification", "Error al mostrar notificación: ${e.message}")
        }
    }

    // Función para enviar vía Cloud Function
    private fun sendViaCloudFunction(
        title: String,
        message: String,
        userIds: List<String>? = null,
        tokens: List<String>? = null,
        sendToAll: Boolean = false
    ) {
        val db = FirebaseFirestore.getInstance()

        val notificationData = hashMapOf<String, Any>(
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "processed" to false
        )

        if (sendToAll) {
            notificationData["topic"] = "all_users"
        } else if (tokens != null && tokens.isNotEmpty()) {
            notificationData["tokens"] = tokens
        } else if (userIds != null && userIds.isNotEmpty()) {
            notificationData["userIds"] = userIds
        }

        // Guardar la solicitud en Firestore para que sea procesada por la Cloud Function
        db.collection("notification_requests")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("FCM", "Solicitud de notificación enviada correctamente: ${it.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error al enviar solicitud de notificación: ${e.message}")
            }
    }

    fun sendToSpecificUsers(
        title: String,
        message: String,
        userIds: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Mostrar notificación local inmediatamente para simular
        sendLocalNotification(title, message)

        // Intentar enviar vía Cloud Function si tenemos tokens
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener tokens para estos usuarios
                val tokens = mutableListOf<String>()
                for (userId in userIds) {
                    val doc = db.collection("tokens").document(userId).get().await()
                    val token = doc.getString("token")
                    if (!token.isNullOrEmpty()) {
                        tokens.add(token)
                    }
                }

                if (tokens.isNotEmpty()) {
                    // Intenta enviar vía Cloud Function
                    sendViaCloudFunction(title, message, userIds = userIds, tokens = tokens)

                }

                // Guardar en Firestore de todos modos
                for (userId in userIds) {
                    val notificationData = hashMapOf(
                        "title" to title,
                        "message" to message,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("notifications")
                        .document(userId)
                        .collection("items")
                        .add(notificationData)
                        .await()
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("LocalNotification", "Error al guardar notificación: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }

    fun sendToAllUsers(
        title: String,
        message: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Mostrar notificación local inmediatamente para simular
        sendLocalNotification(title, message)

        // Intentar enviar vía Cloud Function a todos los usuarios
        sendViaCloudFunction(title, message, sendToAll = true)

        // Guardar en base de datos
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener todos los usuarios
                val snapshot = db.collection("users")
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val batch = db.batch()

                    snapshot.documents.forEach { doc ->
                        val userId = doc.id
                        val notifRef = db.collection("notifications")
                            .document(userId)
                            .collection("items")
                            .document()

                        batch.set(notifRef, hashMapOf(
                            "title" to title,
                            "message" to message,
                            "timestamp" to System.currentTimeMillis()
                        ))
                    }

                    batch.commit().await()
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("LocalNotification", "Error al guardar notificación: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }
}