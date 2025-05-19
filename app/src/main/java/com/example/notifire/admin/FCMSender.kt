// Archivo: app/src/main/java/com/example/notifire/admin/FCMSender.kt

package com.example.notifire.admin

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashMap

class FCMSender(private val context: Context) {

    private val FCM_API = "https://fcm.googleapis.com/fcm/send"
    private val SERVER_KEY = "AAAA-xxxxxxx:APA91bFxxx..." // Deberás reemplazar esto con tu clave de servidor de Firebase

    fun sendToSpecificDevices(
        title: String,
        message: String,
        tokens: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val notification = JSONObject()
            notification.put("title", title)
            notification.put("body", message)

            val jsonObj = JSONObject()
            val tokensArray = JSONArray()
            tokens.forEach { tokensArray.put(it) }

            jsonObj.put("registration_ids", tokensArray)
            jsonObj.put("notification", notification)

            val jsonObjectRequest = object : JsonObjectRequest(
                Request.Method.POST, FCM_API, jsonObj,
                Response.Listener { response ->
                    Log.i("FCM", "Notificación enviada: $response")

                    // Guardar notificaciones en Firestore
                    saveNotificationsToFirestore(title, message, tokens)

                    onSuccess()
                },
                Response.ErrorListener { error ->
                    Log.e("FCM", "Error al enviar notificación: ${error.message}")
                    onError(error.message ?: "Error desconocido")
                }) {
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "key=$SERVER_KEY"
                    headers["Content-Type"] = "application/json"
                    return headers
                }
            }

            Volley.newRequestQueue(context).add(jsonObjectRequest)

        } catch (e: Exception) {
            Log.e("FCM", "Error en solicitud: ${e.message}")
            onError(e.message ?: "Error desconocido")
        }
    }

    fun sendToTopic(
        title: String,
        message: String,
        topic: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val notification = JSONObject()
            notification.put("title", title)
            notification.put("body", message)

            val jsonObj = JSONObject()
            jsonObj.put("to", "/topics/$topic")
            jsonObj.put("notification", notification)

            val jsonObjectRequest = object : JsonObjectRequest(
                Request.Method.POST, FCM_API, jsonObj,
                Response.Listener { response ->
                    Log.i("FCM", "Notificación a tema enviada: $response")

                    // Guardar notificación en Firestore para todos los usuarios
                    saveNotificationToTopic(title, message)

                    onSuccess()
                },
                Response.ErrorListener { error ->
                    Log.e("FCM", "Error al enviar notificación a tema: ${error.message}")
                    onError(error.message ?: "Error desconocido")
                }) {
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "key=$SERVER_KEY"
                    headers["Content-Type"] = "application/json"
                    return headers
                }
            }

            Volley.newRequestQueue(context).add(jsonObjectRequest)

        } catch (e: Exception) {
            Log.e("FCM", "Error en solicitud: ${e.message}")
            onError(e.message ?: "Error desconocido")
        }
    }

    private fun saveNotificationsToFirestore(title: String, message: String, tokens: List<String>) {
        val db = FirebaseFirestore.getInstance()

        // Buscar usuarios que tienen esos tokens
        tokens.forEach { token ->
            db.collection("tokens")
                .whereEqualTo("token", token)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        snapshot.documents.forEach { doc ->
                            val userId = doc.id

                            // Guardar notificación para este usuario
                            val notificationData = hashMapOf(
                                "title" to title,
                                "message" to message,
                                "timestamp" to System.currentTimeMillis()
                            )

                            db.collection("notifications")
                                .document(userId)
                                .collection("items")
                                .add(notificationData)
                                .addOnSuccessListener {
                                    Log.d("FCM", "Notificación guardada para usuario $userId")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FCM", "Error al guardar notificación: ${e.message}")
                                }
                        }
                    }
                }
        }
    }

    private fun saveNotificationToTopic(title: String, message: String) {
        val db = FirebaseFirestore.getInstance()

        // Obtener todos los usuarios
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val batch = db.batch()

                    snapshot.documents.forEach { doc ->
                        val userId = doc.id

                        // Crear referencia a la notificación
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

                    // Ejecutar todas las operaciones como un lote
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("FCM", "Notificaciones guardadas para todos los usuarios")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FCM", "Error al guardar notificaciones: ${e.message}")
                        }
                }
            }
    }
}