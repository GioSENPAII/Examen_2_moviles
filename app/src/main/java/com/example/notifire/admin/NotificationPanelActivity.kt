// Archivo actualizado: app/src/main/java/com/example/notifire/admin/NotificationPanelActivity.kt

package com.example.notifire.admin

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.notifire.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationPanelActivity : AppCompatActivity() {

    private lateinit var userTokens: MutableMap<String, String>
    private lateinit var userIds: MutableMap<String, String>
    private lateinit var userNames: MutableList<String>
    private lateinit var userListView: ListView
    private lateinit var sendButton: Button
    private lateinit var sendAllButton: Button
    private lateinit var titleEditText: EditText
    private lateinit var messageEditText: EditText
    private lateinit var localNotificationManager: LocalNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_panel)

        userTokens = mutableMapOf()
        userIds = mutableMapOf()
        userNames = mutableListOf()

        userListView = findViewById(R.id.userListView)
        sendButton = findViewById(R.id.sendButton)
        sendAllButton = findViewById(R.id.sendAllButton)
        titleEditText = findViewById(R.id.editTextTitle)
        messageEditText = findViewById(R.id.editTextMessage)

        // Inicializar el gestor de notificaciones local
        localNotificationManager = LocalNotificationManager(this)

        loadUsers()

        sendButton.setOnClickListener {
            val selectedUserIds = mutableListOf<String>()
            for (i in 0 until userListView.count) {
                if (userListView.isItemChecked(i)) {
                    userNames[i].let { name ->
                        userIds[name]?.let { userId ->
                            selectedUserIds.add(userId)
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

            if (selectedUserIds.isNotEmpty()) {
                localNotificationManager.sendToSpecificUsers(
                    title = title,
                    message = message,
                    userIds = selectedUserIds,
                    onSuccess = {
                        Toast.makeText(this, "Notificación enviada correctamente", Toast.LENGTH_SHORT).show()
                        titleEditText.text.clear()
                        messageEditText.text.clear()
                        // Desmarcar todas las selecciones
                        for (i in 0 until userListView.count) {
                            userListView.setItemChecked(i, false)
                        }
                    },
                    onError = { error ->
                        Toast.makeText(this, "Error al enviar notificación: $error", Toast.LENGTH_SHORT).show()
                    }
                )
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

            localNotificationManager.sendToAllUsers(
                title = title,
                message = message,
                onSuccess = {
                    Toast.makeText(this, "Notificación enviada a todos los usuarios", Toast.LENGTH_SHORT).show()
                    titleEditText.text.clear()
                    messageEditText.text.clear()
                },
                onError = { error ->
                    Toast.makeText(this, "Error al enviar notificación: $error", Toast.LENGTH_SHORT).show()
                }
            )
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
                                    userIds[name] = uid
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
}