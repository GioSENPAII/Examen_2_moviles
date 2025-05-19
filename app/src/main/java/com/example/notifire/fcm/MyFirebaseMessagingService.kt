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
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notifire.R
import com.example.notifire.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "notifire_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifire Notificaciones"
            val descriptionText = "Canal para notificaciones de Notifire"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM recibido: $token")
        // Guardar el nuevo token en Firestore
        saveTokenToFirestore(token)

        // Suscribirse al tema all_users
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnSuccessListener {
                Log.d(TAG, "Suscrito al tema 'all_users' correctamente")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al suscribirse al tema: ${e.message}")
            }
    }

    private fun saveTokenToFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("tokens").document(user.uid)
                .set(mapOf(
                    "token" to token,
                    "timestamp" to System.currentTimeMillis(),
                    "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL}"
                ))
                .addOnSuccessListener {
                    Log.d(TAG, "Token guardado con éxito")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al guardar token: ${e.message}")
                }
        } else {
            Log.w(TAG, "Usuario no autenticado, token no guardado")
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
                Log.d(TAG, "Notificación guardada en historial: $title")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar notificación: ${e.message}")
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje FCM recibido: ${remoteMessage.notification?.title}")

        // Obtener datos de la notificación
        val title = remoteMessage.notification?.title ?: "Notificación"
        val body = remoteMessage.notification?.body ?: ""

        // Mostrar notificación
        showNotification(title, body)

        // Guardar en historial
        saveNotificationToHistory(title, body)

        // Si hay datos adicionales en el mensaje, procesarlos
        remoteMessage.data.let { data ->
            if (data.isNotEmpty()) {
                Log.d(TAG, "Datos del mensaje: $data")
                // Puedes añadir lógica adicional para procesar datos aquí
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationId = System.currentTimeMillis().toInt()

        try {
            // Verificar permiso de notificaciones
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(this).notify(notificationId, builder.build())
                Log.d(TAG, "Notificación mostrada: $title")
            } else {
                Log.w(TAG, "Permiso de notificaciones no concedido")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar notificación: ${e.message}")
        }
    }
}