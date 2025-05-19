// Archivo: app/src/main/java/com/example/notifire/admin/FCMSender.kt

package com.example.notifire.admin

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FCMSender(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FCMSender"

    fun sendToSpecificUsers(
        title: String,
        message: String,
        userIds: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
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
                    // Crear la solicitud para la Cloud Function
                    val notificationData = hashMapOf(
                        "title" to title,
                        "message" to message,
                        "tokens" to tokens,
                        "processed" to false,
                        "timestamp" to System.currentTimeMillis()
                    )

                    // Guardar en Firestore para que sea procesado por la Cloud Function
                    db.collection("notification_requests")
                        .add(notificationData)
                        .await()

                    Log.d(TAG, "Solicitud de notificación enviada correctamente")
                } else {
                    Log.w(TAG, "No se encontraron tokens válidos para los usuarios seleccionados")
                }

                // Guardar las notificaciones en la colección de notificaciones para cada usuario
                for (userId in userIds) {
                    val notifData = hashMapOf(
                        "title" to title,
                        "message" to message,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("notifications")
                        .document(userId)
                        .collection("items")
                        .add(notifData)
                        .await()
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al enviar notificación: ${e.message}", e)
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Crear la solicitud para la Cloud Function usando un tema
                val notificationData = hashMapOf(
                    "title" to title,
                    "message" to message,
                    "topic" to "all_users",
                    "processed" to false,
                    "timestamp" to System.currentTimeMillis()
                )

                // Guardar en Firestore para que sea procesado por la Cloud Function
                db.collection("notification_requests")
                    .add(notificationData)
                    .await()

                // Obtener todos los usuarios para guardar en sus colecciones
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

                Log.d(TAG, "Solicitud de notificación a todos enviada correctamente")

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al enviar notificación a todos: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }
}