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
        // En el método onclick del sendButton:

        sendButton.setOnClickListener {
            val selectedUserIds = mutableListOf<String>()

            // Obtener los usuarios seleccionados
            for (i in 0 until userListView.count) {
                if (userListView.isItemChecked(i)) {
                    val userName = userNames[i]
                    val userId = userIds[userName]
                    if (userId != null) {
                        selectedUserIds.add(userId)
                        Log.d("NotificationPanel", "Usuario seleccionado: $userName ($userId)")
                    }
                }
            }

            val title = titleEditText.text.toString()
            val message = messageEditText.text.toString()

            Log.d("NotificationPanel", "Título: $title, Mensaje: $message, Usuarios seleccionados: ${selectedUserIds.size}")

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Debes completar el título y mensaje", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedUserIds.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE

                // 1. Mostrar notificación local inmediatamente como respaldo
                localNotificationManager.sendLocalNotification(title, message)

                // 2. Intentar enviar vía FCM (Cloud Functions)
                fcmSender.sendToSpecificUsers(
                    title = title,
                    message = message,
                    userIds = selectedUserIds,
                    onSuccess = {
                        Log.d("NotificationPanel", "Notificación enviada vía FCM correctamente")
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
                        Log.e("NotificationPanel", "Error al enviar vía FCM: $error")
                        // Falló el FCM pero la notificación local ya se mostró
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Notificación enviada solo localmente", Toast.LENGTH_SHORT).show()
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

        Log.d("NotificationPanel", "Comenzando carga de usuarios. Usuario actual: $currentUid")

        userNames.clear() // Limpia las listas antes de cargar
        userTokens.clear()
        userIds.clear()

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                Log.d("NotificationPanel", "Obtenidos ${result.size()} usuarios de Firestore")

                if (result.isEmpty) {
                    Log.w("NotificationPanel", "No se encontraron usuarios en la base de datos")
                    Toast.makeText(this, "No hay usuarios registrados", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (doc in result) {
                    val uid = doc.id // Usar el ID del documento directamente
                    val name = doc.getString("name") ?: "Usuario"
                    val role = doc.getString("role") ?: "user"

                    Log.d("NotificationPanel", "Usuario encontrado: $name (ID: $uid, Rol: $role)")

                    if (uid != currentUid) {
                        // Agregar el usuario a la lista inmediatamente
                        userNames.add(name)
                        userIds[name] = uid

                        // Buscar token del usuario
                        db.collection("tokens").document(uid).get()
                            .addOnSuccessListener { tokenDoc ->
                                val token = tokenDoc.getString("token")
                                Log.d("NotificationPanel", "Token para $name: ${token ?: "No encontrado"}")

                                if (!token.isNullOrEmpty()) {
                                    userTokens[name] = token
                                    // Actualizar la vista después de obtener el token
                                    updateUserListView()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("NotificationPanel", "Error al obtener token para $name: ${e.message}")
                            }

                        // Actualizar la vista para no esperar por los tokens
                        updateUserListView()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("NotificationPanel", "Error al cargar usuarios: ${e.message}", e)
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