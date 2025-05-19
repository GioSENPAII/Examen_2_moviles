// Archivo: app/src/main/java/com/example/notifire/fcm/MyFirebaseMessagingService.kt

package com.example.notifire.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notifire.R
import com.example.notifire.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardar el nuevo token en Firestore
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("tokens").document(user.uid)
                .set(mapOf("token" to token, "timestamp" to System.currentTimeMillis()))
                .addOnSuccessListener {
                    println("Token guardado con éxito")
                }
                .addOnFailureListener { e ->
                    println("Error al guardar token: ${e.message}")
                }
        }
    }

    private fun saveNotificationToHistory(title: String, message: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val notif = mapOf(
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("notifications").document(user.uid)
            .collection("items")
            .add(notif)
            .addOnSuccessListener {
                println("Notificación guardada en historial")
            }
            .addOnFailureListener { e ->
                println("Error al guardar notificación: ${e.message}")
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Obtener datos de la notificación
        val title = remoteMessage.notification?.title ?: "Notificación"
        val body = remoteMessage.notification?.body ?: ""

        // Mostrar notificación
        showNotification(title, body)

        // Guardar en historial
        saveNotificationToHistory(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "notifire_channel"
        val notificationId = (System.currentTimeMillis() % 10000).toInt()

        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = NotificationManagerCompat.from(this)

        // Crear canal de notificación en Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifire Notificaciones",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Verificar permiso de notificaciones
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(notificationId, builder.build())
        }
    }
}