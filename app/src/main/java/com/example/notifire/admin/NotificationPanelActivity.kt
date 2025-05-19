package com.example.notifire.admin

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.notifire.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

class NotificationPanelActivity : AppCompatActivity() {

    private lateinit var userTokens: MutableList<String>
    private lateinit var userNames: MutableList<String>
    private lateinit var userListView: ListView
    private lateinit var sendButton: Button
    private lateinit var titleEditText: EditText
    private lateinit var messageEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_panel)

        userTokens = mutableListOf()
        userNames = mutableListOf()

        userListView = findViewById(R.id.userListView)
        sendButton = findViewById(R.id.sendButton)
        titleEditText = findViewById(R.id.editTextTitle)
        messageEditText = findViewById(R.id.editTextMessage)

        loadUsers()

        sendButton.setOnClickListener {
            val selectedTokens = mutableListOf<String>()
            for (i in 0 until userListView.count) {
                if (userListView.isItemChecked(i)) {
                    selectedTokens.add(userTokens[i])
                }
            }

            val title = titleEditText.text.toString()
            val message = messageEditText.text.toString()

            if (selectedTokens.isNotEmpty()) {
                sendNotification(title, message, selectedTokens)
            } else {
                Toast.makeText(this, "Selecciona al menos un usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUsers() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereEqualTo("role", "user")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val uid = doc.getString("uid")
                    val name = doc.getString("name")

                    if (uid != null && uid != currentUid) {
                        db.collection("tokens").document(uid).get()
                            .addOnSuccessListener { tokenDoc ->
                                val token = tokenDoc.getString("token")
                                if (!token.isNullOrEmpty()) {
                                    userNames.add(name ?: "Usuario")
                                    userTokens.add(token)
                                    updateUserListView()
                                }
                            }
                    }
                }
            }
    }

    private fun updateUserListView() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, userNames)
        userListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        userListView.adapter = adapter
    }

    private fun sendNotification(title: String, message: String, tokens: List<String>) {
        val json = JSONObject().apply {
            put("title", title)
            put("body", message)
            put("tokens", JSONArray(tokens))
        }

        val url = "https://example.com/sendNotification" // URL falsa para simular

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                Log.d("Notification", "Éxito: $response")
                Toast.makeText(this, "Notificación enviada", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Log.e("Notification", "Error: ${error.message}")
                Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}
