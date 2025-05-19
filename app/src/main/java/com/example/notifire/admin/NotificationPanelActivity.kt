// Archivo: app/src/main/java/com/example/notifire/admin/NotificationPanelActivity.kt

package com.example.notifire.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
    private lateinit var fcmSender: FCMSender
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_panel)

        // Inicialización de colecciones
        userTokens = mutableMapOf()
        userIds = mutableMapOf()
        userNames = mutableListOf()

        // Referencias a vistas
        userListView = findViewById(R.id.userListView)
        sendButton = findViewById(R.id.sendButton)
        sendAllButton = findViewById(R.id.sendAllButton)
        titleEditText = findViewById(R.id.editTextTitle)
        messageEditText = findViewById(R.id.editTextMessage)
        progressBar = findViewById(R.id.progressBar)

        // Si no existe el progressBar, añadirlo programáticamente
        if (!::progressBar.isInitialized) {
            progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            progressBar.layoutParams = layoutParams
            progressBar.visibility = View.GONE
            // Añadirlo al layout
            val rootView = findViewById<LinearLayout>(R.id.rootLayout)
            // Si no existe rootLayout, usa el ViewGroup principal
            if (rootView != null) {
                rootView.addView(progressBar, 0)
            }
        }

        // Inicializar gestores de notificaciones
        localNotificationManager = LocalNotificationManager(this)
        fcmSender = FCMSender(this)

        // Cargar usuarios
        loadUsers()

        // Configurar listener para enviar a usuarios seleccionados
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
                progressBar.visibility = View.VISIBLE

                // 1. Intentar enviar vía FCM (Cloud Functions)
                fcmSender.sendToSpecificUsers(
                    title = title,
                    message = message,
                    userIds = selectedUserIds,
                    onSuccess = {
                        Log.d("NotificationPanel", "Notificación enviada vía FCM correctamente")
                    },
                    onError = { error ->
                        Log.e("NotificationPanel", "Error al enviar vía FCM: $error")
                    }
                )

                // 2. Enviar también notificación local como respaldo
                localNotificationManager.sendToSpecificUsers(
                    title = title,
                    message = message,
                    userIds = selectedUserIds,
                    onSuccess = {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Notificación enviada correctamente", Toast.LENGTH_SHORT).show()
                        titleEditText.text.clear()
                        messageEditText.text.clear()
                        // Desmarcar todas las selecciones
                        for (i in 0 until userListView.count) {
                            userListView.setItemChecked(i, false)
                        }
                    },
                    onError = { error ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error al enviar notificación: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(this, "Selecciona al menos un usuario", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar listener para enviar a todos los usuarios
        sendAllButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val message = messageEditText.text.toString()

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Debes completar el título y mensaje", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            // 1. Intentar enviar vía FCM a todos (usando tema)
            fcmSender.sendToAllUsers(
                title = title,
                message = message,
                onSuccess = {
                    Log.d("NotificationPanel", "Notificación enviada a todos vía FCM correctamente")
                },
                onError = { error ->
                    Log.e("NotificationPanel", "Error al enviar a todos vía FCM: $error")
                }
            )

            // 2. Enviar también notificación local como respaldo
            localNotificationManager.sendToAllUsers(
                title = title,
                message = message,
                onSuccess = {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Notificación enviada a todos los usuarios", Toast.LENGTH_SHORT).show()
                    titleEditText.text.clear()
                    messageEditText.text.clear()
                },
                onError = { error ->
                    progressBar.visibility = View.GONE
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
                        // Buscar token del usuario
                        db.collection("tokens").document(uid).get()
                            .addOnSuccessListener { tokenDoc ->
                                val token = tokenDoc.getString("token")
                                if (!token.isNullOrEmpty()) {
                                    userNames.add(name)
                                    userTokens[name] = token
                                    userIds[name] = uid
                                    updateUserListView()
                                } else {
                                    // Incluso sin token, guardar usuario para enviar notificación local
                                    userNames.add(name)
                                    userIds[name] = uid
                                    updateUserListView()
                                }
                            }
                            .addOnFailureListener {
                                // Incluso sin token, guardar usuario para enviar notificación local
                                userNames.add(name)
                                userIds[name] = uid
                                updateUserListView()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar usuarios: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserListView() {
        // Adaptador personalizado para mostrar más información
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            userNames
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)

                // Obtener el nombre de usuario
                val userName = userNames[position]

                // Mostrar si tiene token FCM disponible
                val hasToken = userTokens.containsKey(userName)
                if (hasToken) {
                    textView.text = "$userName ✓"
                } else {
                    textView.text = userName
                }

                return view
            }
        }

        userListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        userListView.adapter = adapter
    }
}