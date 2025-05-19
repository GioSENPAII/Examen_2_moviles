// Archivo: app/src/main/java/com/example/notifire/admin/NotificationPanelActivity.kt

package com.example.notifire.admin

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.notifire.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import org.json.JSONArray
import org.json.JSONObject

class NotificationPanelActivity : AppCompatActivity() {

    private lateinit var userTokens: MutableMap<String, String>
    private lateinit var userNames: MutableList<String>
    private lateinit var userListView: ListView
    private lateinit var sendButton: Button
    private lateinit var sendAllButton: Button
    private lateinit var titleEditText: EditText
    private lateinit var messageEditText: EditText
    private lateinit var functions: FirebaseFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_panel)

        userTokens = mutableMapOf()
        userNames = mutableListOf()

        userListView = findViewById(R.id.userListView)
        sendButton = findViewById(R.id.sendButton)
        sendAllButton = findViewById(R.id.sendAllButton)
        titleEditText = findViewById(R.id.editTextTitle)
        messageEditText = findViewById(R.id.editTextMessage)

        // Inicializar Firebase Functions
        functions = FirebaseFunctions.getInstance()

        loadUsers()

        sendButton.setOnClickListener {
            val selectedTokens = mutableListOf<String>()
            for (i in 0 until userListView.count) {
                if (userListView.isItemChecked(i)) {
                    userNames[i].let { name ->
                        userTokens[name]?.let { token ->
                            selectedTokens.add(token)
                        }
                    }
                }
            }

            val title = titleEditText.text.toString()
            val message = messageEditText.text.toString()

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Debes completar el título y mensaje", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedTokens.isNotEmpty()) {
                sendNotification(title, message, selectedTokens)
            } else {
                Toast.makeText(this, "Selecciona al menos un usuario", Toast.LENGTH_SHORT).show()
            }
        }

        sendAllButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val message = messageEditText.text.toString()

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Debes completar el título y mensaje", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendNotificationToAll(title, message)
        }
    }

    private fun loadUsers() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val uid = doc.getString("uid") ?: continue
                    val name = doc.getString("name") ?: "Usuario"
                    val role = doc.getString("role") ?: "user"

                    if (uid != currentUid) {
                        db.collection("tokens").document(uid).get()
                            .addOnSuccessListener { tokenDoc ->
                                val token = tokenDoc.getString("token")
                                if (!token.isNullOrEmpty()) {
                                    userNames.add(name)
                                    userTokens[name] = token
                                    updateUserListView()
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar usuarios: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserListView() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, userNames)
        userListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        userListView.adapter = adapter
    }

    private fun sendNotification(title: String, message: String, tokens: List<String>) {
        val data = hashMapOf(
            "title" to title,
            "message" to message,
            "tokens" to tokens
        )

        functions.getHttpsCallable("sendNotification")
            .call(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Notificación enviada correctamente", Toast.LENGTH_SHORT).show()
                titleEditText.text.clear()
                messageEditText.text.clear()
                // Desmarcar todas las selecciones
                for (i in 0 until userListView.count) {
                    userListView.setItemChecked(i, false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al enviar notificación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendNotificationToAll(title: String, message: String) {
        val data = hashMapOf(
            "title" to title,
            "message" to message,
            "topic" to "all_users"
        )

        functions.getHttpsCallable("sendNotificationToTopic")
            .call(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Notificación enviada a todos los usuarios", Toast.LENGTH_SHORT).show()
                titleEditText.text.clear()
                messageEditText.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al enviar notificación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}